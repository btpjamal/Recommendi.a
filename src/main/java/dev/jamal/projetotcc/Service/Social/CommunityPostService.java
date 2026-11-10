package dev.jamal.projetotcc.Service.Social;

import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CommunityPostService {
    private final CommunityPostRepository posts;
    private final CommunityPostImageRepository images;
    private final CommunityRepository communities;
    private final CommunityMemberRepository members;
    private final UserRepository users;
    private final SocialProfileRepository profiles;
    private final SocialUserBlockRepository blocks;
    private final CommunityChatLockRepository locks;
    @PersistenceContext private EntityManager entityManager;

    private ResponseStatusException erro(HttpStatus status,String text) {return new ResponseStatusException(status,text);}
    private ResponseStatusException indisponivel() {return erro(HttpStatus.NOT_FOUND,"Publicação indisponível.");}
    private boolean adulto(User user) {
        return user.getDataNascimento()!=null && !user.getDataNascimento().isAfter(LocalDate.now().minusYears(18));
    }

    private User acesso(Long communityId,Long userId,boolean bloquear) {
        // Same user -> community order as join/leave and chat; sends commit before the next post obtains an ID.
        if(bloquear) {
            if(locks.lockUser(userId).isEmpty()) throw erro(HttpStatus.FORBIDDEN,"Participação indisponível.");
            if(locks.lockCommunity(communityId).isEmpty()) throw erro(HttpStatus.NOT_FOUND,"Comunidade indisponível.");
        }
        var user=users.findById(userId).orElseThrow(()->erro(HttpStatus.FORBIDDEN,"Participação indisponível."));
        var profile=profiles.findByUser_Id(userId).orElseThrow(()->erro(HttpStatus.FORBIDDEN,"Ative sua participação social."));
        if(bloquear) {entityManager.refresh(user);entityManager.refresh(profile);}
        if(!adulto(user) || !profile.isEnabled()) throw erro(HttpStatus.FORBIDDEN,"Sua participação social está indisponível.");
        if(!members.existsByCommunity_IdAndUser_Id(communityId,userId))
            throw erro(HttpStatus.FORBIDDEN,"Entre na comunidade para acessar as publicações.");
        return user;
    }

    @Transactional(readOnly=true)
    public void validarAcesso(Long communityId,Long userId) {acesso(communityId,userId,false);}

    @Transactional(readOnly=true)
    public CommunityPostPageDTO listar(Long communityId,Long viewer,Long antesDe,int limite) {
        acesso(communityId,viewer,false);
        if((antesDe!=null && antesDe<=0) || limite<1 || limite>30)
            throw erro(HttpStatus.BAD_REQUEST,"Use um cursor válido e limite de 1 a 30.");
        var rows=posts.buscarVisiveis(communityId,viewer,antesDe==null?Long.MAX_VALUE:antesDe,
            LocalDate.now().minusYears(18),PageRequest.of(0,limite+1));
        var page=rows.stream().limit(limite).map(p->dto(p,viewer)).toList();
        return new CommunityPostPageDTO(page,rows.size()>limite,page.isEmpty()?null:page.get(page.size()-1).id());
    }

    @Transactional
    public CommunityPostDTO criar(Long communityId,Long userId,CreateCommunityPostDTO request,byte[] image) {
        var user=acesso(communityId,userId,true);
        if(request==null || request.clientId()==null) throw erro(HttpStatus.BAD_REQUEST,"Informe um identificador de envio.");
        String text=texto(request.conteudo());conteudoValido(text,image!=null);
        String fingerprint=hash(text,image);
        var existing=posts.findByUser_IdAndClientId(userId,request.clientId());
        if(existing.isPresent()) {
            var p=existing.get();
            if(p.getDeletedAt()!=null || p.getHiddenAt()!=null || !p.getCommunity().getId().equals(communityId) || !p.getRequestHash().equals(fingerprint))
                throw erro(HttpStatus.CONFLICT,"O identificador de envio já foi usado. Atualize a lista antes de tentar novamente.");
            return dto(p,userId);
        }
        var p=new CommunityPost();p.setCommunity(communities.getReferenceById(communityId));p.setUser(user);
        p.setClientId(request.clientId());p.setRequestHash(fingerprint);p.setConteudo(text);
        p.setPossuiImagem(image!=null);p.setCreatedAt(Instant.now());
        posts.saveAndFlush(p);
        if(image!=null) guardarImagem(p.getId(),image);
        return dto(p,userId);
    }

    @Transactional
    public CommunityPostDTO editar(Long communityId,Long userId,Long postId,EditCommunityPostDTO request,byte[] image) {
        acesso(communityId,userId,true);
        var p=propria(communityId,userId,postId);
        if(p.getHiddenAt()!=null) throw indisponivel();
        versao(p,request==null?null:request.versao());
        if(image!=null && request.removerImagem()) throw erro(HttpStatus.BAD_REQUEST,"Escolha substituir ou remover a imagem.");
        String text=texto(request.conteudo());
        boolean hasImage=image!=null || (p.isPossuiImagem() && !request.removerImagem());
        conteudoValido(text,hasImage);
        p.setConteudo(text);p.setPossuiImagem(hasImage);p.setEditedAt(Instant.now());
        if(image!=null) guardarImagem(postId,image);
        else if(request.removerImagem()) images.deleteById(postId);
        posts.saveAndFlush(p);
        return dto(p,userId);
    }

    @Transactional
    public void excluir(Long communityId,Long userId,Long postId,Long version) {
        acesso(communityId,userId,true);
        var p=propria(communityId,userId,postId);versao(p,version);
        p.setDeletedAt(Instant.now());p.setConteudo("");p.setPossuiImagem(false);
        images.deleteById(postId);
        // Tombstone retains only identity/hash/timestamps, preventing an old retry from recreating a deleted post.
        posts.saveAndFlush(p);
    }

    @Transactional(readOnly=true)
    public byte[] imagem(Long communityId,Long viewer,Long postId) {
        acesso(communityId,viewer,false);
        var p=posts.findByIdAndCommunity_Id(postId,communityId).filter(x->x.getDeletedAt()==null && x.getHiddenAt()==null)
            .orElseThrow(this::indisponivel);
        Long author=p.getUser().getId();
        if(!p.isPossuiImagem() || !adulto(p.getUser()) || blocks.bloqueados(viewer,author) ||
            !profiles.findByUser_Id(author).map(SocialProfile::isEnabled).orElse(false)) throw indisponivel();
        return images.findById(postId).map(CommunityPostImage::getDados).orElseThrow(this::indisponivel);
    }

    @Transactional(readOnly=true)
    public CommunityPostDTO buscar(Long community,Long viewer,Long postId) {
        return dto(exigirVisivel(community,viewer,postId,false),viewer);
    }

    @Transactional
    public CommunityPost exigirVisivel(Long community,Long viewer,Long postId,boolean escrita) {
        acesso(community,viewer,escrita);
        var p=posts.findByIdAndCommunity_Id(postId,community).orElseThrow(this::indisponivel);
        if(escrita) entityManager.refresh(p);
        if(p.getDeletedAt()!=null || p.getHiddenAt()!=null || !adulto(p.getUser()) || blocks.bloqueados(viewer,p.getUser().getId()) ||
            !profiles.findByUser_Id(p.getUser().getId()).map(SocialProfile::isEnabled).orElse(false)) throw indisponivel();
        return p;
    }

    private CommunityPost propria(Long community,Long user,Long id) {
        var p=posts.findByIdAndCommunity_Id(id,community).orElseThrow(this::indisponivel);
        entityManager.refresh(p);
        if(p.getDeletedAt()!=null || !p.getUser().getId().equals(user)) throw indisponivel();
        return p;
    }
    private void versao(CommunityPost p,Long version) {
        if(version==null || version<0) throw erro(HttpStatus.BAD_REQUEST,"Informe a versão da publicação.");
        if(!version.equals(p.getVersion())) throw erro(HttpStatus.CONFLICT,"Esta publicação mudou. Atualize a lista antes de editar ou excluir.");
    }
    private String texto(String value) {
        if(value!=null && value.indexOf('\0')>=0) throw erro(HttpStatus.BAD_REQUEST,"O texto contém um caractere inválido.");
        if(value!=null && value.length()>4000) throw erro(HttpStatus.BAD_REQUEST,"O texto deve ter até 4000 caracteres.");
        return value==null?"":value.strip();
    }
    private void conteudoValido(String text,boolean image) {
        if(text.isBlank() && !image) throw erro(HttpStatus.BAD_REQUEST,"Escreva um texto ou envie uma imagem.");
    }
    private void guardarImagem(Long id,byte[] image) {
        var row=new CommunityPostImage();row.setPostId(id);row.setDados(image);images.save(row);
    }
    private String hash(String text,byte[] image) {
        try {
            var digest=MessageDigest.getInstance("SHA-256");digest.update(text.getBytes(StandardCharsets.UTF_8));
            digest.update((byte)0); if(image!=null)digest.update(image);
            return HexFormat.of().formatHex(digest.digest());
        } catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    private CommunityPostDTO dto(CommunityPost p,Long viewer) {
        return new CommunityPostDTO(p.getId(),p.getCommunity().getId(),p.getUser().getId(),p.getUser().getNome(),
            p.getConteudo(),p.isPossuiImagem(),p.getCreatedAt(),p.getEditedAt(),p.getVersion(),p.getUser().getId().equals(viewer));
    }
}
