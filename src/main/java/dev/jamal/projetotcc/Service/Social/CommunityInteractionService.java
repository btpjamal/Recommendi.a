package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;
import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Repository.*;
import dev.jamal.projetotcc.Enum.CommunityReportStatus;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;import java.util.*;import java.nio.charset.StandardCharsets;import java.security.*;

@Service @RequiredArgsConstructor @Transactional
public class CommunityInteractionService {
 private final CommunityPostService postAccess;
 private final CommunityCommentRepository comments;
 private final CommunityPostLikeRepository likes;
 private final CommunityReportRepository reports;
 private final UserRepository users;
 private final SocialProfileRepository profiles;
 private final SocialUserBlockRepository blocks;
 private final org.springframework.context.ApplicationEventPublisher publisher;
 @PersistenceContext private EntityManager entityManager;
 private ResponseStatusException error(HttpStatus s,String text){return new ResponseStatusException(s,text);}
 private ResponseStatusException absent(){return error(HttpStatus.NOT_FOUND,"Comentário indisponível.");}
 private LocalDate cutoff(){return LocalDate.now().minusYears(18);}

 @Transactional(readOnly=true)
 public CommunityInteractionDTO resumo(Long community,Long viewer,Long post) {
  postAccess.exigirVisivel(community,viewer,post,false);return summary(viewer,post);
 }
 private CommunityInteractionDTO summary(Long viewer,Long post) {
  return new CommunityInteractionDTO(likes.contar(post,viewer,cutoff()),likes.existsByPostIdAndUserId(post,viewer),comments.contar(post,viewer,cutoff()));
 }
 public CommunityInteractionDTO curtir(Long community,Long viewer,Long post,boolean desired) {
  postAccess.exigirVisivel(community,viewer,post,true);
  if(desired && !likes.existsByPostIdAndUserId(post,viewer)) {
   var l=new CommunityPostLike();l.setPostId(post);l.setUserId(viewer);l.setCreatedAt(Instant.now());likes.saveAndFlush(l);
  } else if(!desired){likes.deleteByPostIdAndUserId(post,viewer);likes.flush();}
  return summary(viewer,post);
 }
 @Transactional(readOnly=true)
 public CommunityCommentPageDTO listar(Long community,Long viewer,Long post,Long before,int limit) {
  postAccess.exigirVisivel(community,viewer,post,false);
  if((before!=null&&before<=0)||limit<1||limit>30)throw error(HttpStatus.BAD_REQUEST,"Cursor inválido ou limite fora de 1 a 30.");
  var found=comments.visiveis(post,viewer,cutoff(),before==null?Long.MAX_VALUE:before,PageRequest.of(0,limit+1));
  var page=found.stream().limit(limit).map(c->dto(c,viewer)).toList();
  return new CommunityCommentPageDTO(page,found.size()>limit,page.isEmpty()?null:page.get(page.size()-1).id());
 }
 @Transactional(readOnly=true)
 public CommunityCommentDTO buscarComentario(Long community,Long viewer,Long post,Long id) {
  postAccess.exigirVisivel(community,viewer,post,false);
  var c=comments.findByIdAndPost_Id(id,post).orElseThrow(this::absent);Long author=c.getUser().getId();
  if(c.getDeletedAt()!=null||c.getHiddenAt()!=null||blocks.bloqueados(viewer,author)||c.getUser().getDataNascimento()==null||c.getUser().getDataNascimento().isAfter(cutoff())||
    !profiles.findByUser_Id(author).map(SocialProfile::isEnabled).orElse(false))throw absent();
  return dto(c,viewer);
 }
 public CommunityCommentDTO comentar(Long community,Long viewer,Long post,CreateCommunityCommentDTO request) {
  var parent=postAccess.exigirVisivel(community,viewer,post,true);
  if(request==null||request.clientId()==null)throw error(HttpStatus.BAD_REQUEST,"Informe o identificador de envio.");
  String text=text(request.conteudo(),1000,true);String hash=hash(text);
  var existing=comments.findByUser_IdAndClientId(viewer,request.clientId());
  if(existing.isPresent()) {
   var c=existing.get();
   if(!c.getPost().getId().equals(post)||c.getDeletedAt()!=null||c.getHiddenAt()!=null||!hash.equals(c.getRequestHash()))
    throw error(HttpStatus.CONFLICT,"Este identificador já foi usado. Confira os comentários antes de reenviar.");
   return dto(c,viewer);
  }
  var c=new CommunityComment();c.setPost(parent);c.setUser(users.getReferenceById(viewer));c.setClientId(request.clientId());
  c.setRequestHash(hash);c.setConteudo(text);c.setCreatedAt(Instant.now());comments.saveAndFlush(c);
  if(!parent.getUser().getId().equals(viewer)) publisher.publishEvent(new SocialNotificationEvent(dev.jamal.projetotcc.Enum.SocialNotificationType.COMENTARIO_PUBLICACAO,c.getId(),null));
  return dto(c,viewer);
 }
 public CommunityCommentDTO editar(Long community,Long viewer,Long post,Long id,EditCommunityCommentDTO request) {
  postAccess.exigirVisivel(community,viewer,post,true);var c=own(post,id,viewer);
  if(c.getHiddenAt()!=null)throw absent();
  version(c,request==null?null:request.versao());c.setConteudo(text(request.conteudo(),1000,true));c.setEditedAt(Instant.now());
  comments.saveAndFlush(c);return dto(c,viewer);
 }
 public void excluir(Long community,Long viewer,Long post,Long id,Long version) {
  postAccess.exigirVisivel(community,viewer,post,true);var c=own(post,id,viewer);version(c,version);
  c.setConteudo("");c.setDeletedAt(Instant.now());comments.saveAndFlush(c);
 }
 private CommunityComment own(Long post,Long id,Long viewer) {
  var c=comments.findByIdAndPost_Id(id,post).orElseThrow(this::absent);entityManager.refresh(c);
  if(c.getDeletedAt()!=null||!c.getUser().getId().equals(viewer))throw absent();return c;
 }
 private void version(CommunityComment c,Long version) {
  if(version==null||version<0)throw error(HttpStatus.BAD_REQUEST,"Informe a versão do comentário.");
  if(!version.equals(c.getVersion()))throw error(HttpStatus.CONFLICT,"O comentário mudou. Atualize antes de editar ou excluir.");
 }
 public CommunityReportReceiptDTO denunciar(Long community,Long viewer,Long post,Long comment,CreateCommunityReportDTO request) {
  var p=postAccess.exigirVisivel(community,viewer,post,true);
  if(request==null||request.motivo()==null)throw error(HttpStatus.BAD_REQUEST,"Informe o motivo da denúncia.");
  String details=text(request.detalhes(),500,false);
  Long author=p.getUser().getId(),version=p.getVersion();
  if(comment!=null) {
   var c=comments.findByIdAndPost_Id(comment,post).orElseThrow(this::absent);entityManager.refresh(c);
   author=c.getUser().getId();version=c.getVersion();
   if(c.getDeletedAt()!=null||c.getHiddenAt()!=null||blocks.bloqueados(viewer,author)||
    c.getUser().getDataNascimento()==null||c.getUser().getDataNascimento().isAfter(cutoff())||
    !profiles.findByUser_Id(author).map(SocialProfile::isEnabled).orElse(false))throw absent();
  }
  if(author.equals(viewer))throw error(HttpStatus.BAD_REQUEST,"Você pode editar ou excluir o próprio conteúdo.");
  if(request.versao()==null||request.versao()<0)throw error(HttpStatus.BAD_REQUEST,"Informe a versão do conteúdo denunciado.");
  if(!version.equals(request.versao()))throw error(HttpStatus.CONFLICT,"O conteúdo mudou. Atualize antes de denunciar.");
  var old=reports.findByReporterIdAndPost_IdAndCommentIdAndTargetVersion(viewer,post,comment,version);
  if(old.isPresent())return new CommunityReportReceiptDTO(old.get().getId());
  var r=new CommunityReport();r.setPost(p);r.setCommentId(comment);r.setReporterId(viewer);r.setTargetVersion(version);
  r.setReason(request.motivo());r.setDetalhes(details);r.setStatus(CommunityReportStatus.ABERTA);r.setCreatedAt(Instant.now());
  reports.saveAndFlush(r);return new CommunityReportReceiptDTO(r.getId());
 }
 private String text(String value,int max,boolean required) {
  if(value!=null&&(value.length()>max||value.indexOf('\0')>=0))throw error(HttpStatus.BAD_REQUEST,"Texto inválido ou acima do limite de "+max+" caracteres.");
  String result=value==null?"":value.strip();if(required&&result.isBlank())throw error(HttpStatus.BAD_REQUEST,"Escreva um comentário.");return result;
 }
 private String hash(String text) {
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
  catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 private CommunityCommentDTO dto(CommunityComment c,Long viewer) {
  return new CommunityCommentDTO(c.getId(),c.getUser().getId(),c.getUser().getNome(),c.getConteudo(),c.getCreatedAt(),c.getEditedAt(),c.getVersion(),c.getUser().getId().equals(viewer));
 }
}
