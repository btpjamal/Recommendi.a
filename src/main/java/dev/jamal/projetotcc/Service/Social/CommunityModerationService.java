package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Entities.*;
import dev.jamal.projetotcc.Enum.*;import dev.jamal.projetotcc.Repository.*;
import jakarta.persistence.*;import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service @RequiredArgsConstructor @Transactional @PreAuthorize("hasRole('ADMIN')")
public class CommunityModerationService {
 private final CommunityReportRepository reports;private final CommunityCommentRepository comments;
 private final CommunityPostRepository posts;private final CommunityPostImageRepository images;
 private final UserRepository users;private final CommunityChatLockRepository locks;
 @PersistenceContext private EntityManager entityManager;
 private ResponseStatusException error(HttpStatus s,String text){return new ResponseStatusException(s,text);}
 private void admin(Long id){
  if(!users.findById(id).map(u->u.getRole()==Role.ADMIN).orElse(false))throw error(HttpStatus.FORBIDDEN,"Acesso administrativo necessário.");
 }
 @Transactional(readOnly=true)
 public CommunityReportPageDTO listar(Long admin,CommunityReportStatus status,Long before,int limit) {
  admin(admin);if(status==null||(before!=null&&before<=0)||limit<1||limit>30)throw error(HttpStatus.BAD_REQUEST,"Filtro ou paginação inválidos.");
  var rows=reports.findByStatusAndIdLessThanOrderByIdDesc(status,before==null?Long.MAX_VALUE:before,PageRequest.of(0,limit+1));
  var page=rows.stream().limit(limit).map(this::dto).toList();
  return new CommunityReportPageDTO(page,rows.size()>limit,page.isEmpty()?null:page.get(page.size()-1).id());
 }
 public CommunityReportDTO analisar(Long admin,Long id,ReviewCommunityReportDTO request) {
  admin(admin);var r=reports.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Denúncia indisponível."));
  if(locks.lockUser(admin).isEmpty()||locks.lockCommunity(r.getPost().getCommunity().getId()).isEmpty())throw error(HttpStatus.NOT_FOUND,"Comunidade indisponível.");
  entityManager.refresh(users.findById(admin).orElseThrow());admin(admin);
  entityManager.refresh(r);var p=r.getPost();entityManager.refresh(p);
  var c=r.getCommentId()==null?null:comments.findById(r.getCommentId()).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Comentário indisponível."));
  if(c!=null)entityManager.refresh(c);
  Long current=c==null?p.getVersion():c.getVersion();
  if(request==null||request.decisao()==null||request.notas()==null||request.notas().isBlank()||request.notas().length()>1000||request.notas().indexOf('\0')>=0)
   throw error(HttpStatus.BAD_REQUEST,"Informe a decisão e uma justificativa de até 1000 caracteres.");
  if(r.getStatus()!=CommunityReportStatus.ABERTA||!r.getVersion().equals(request.versaoDenuncia())||!current.equals(request.versaoConteudo()))
   throw error(HttpStatus.CONFLICT,"A denúncia ou o conteúdo mudou. Atualize antes de analisar.");
  if(request.decisao()==CommunityModerationDecision.OCULTAR) {
   if(p.getDeletedAt()!=null||(c!=null&&c.getDeletedAt()!=null))throw error(HttpStatus.CONFLICT,"O conteúdo já foi excluído. Encerre mantendo o estado atual.");
   if(c==null){if(p.getHiddenAt()==null){p.setHiddenAt(Instant.now());posts.saveAndFlush(p);}}
   else if(c.getHiddenAt()==null){c.setHiddenAt(Instant.now());comments.saveAndFlush(c);}
  }
  r.setStatus(CommunityReportStatus.ANALISADA);r.setDecision(request.decisao());r.setNotas(request.notas().strip());
  r.setReviewerId(admin);r.setReviewedAt(Instant.now());reports.saveAndFlush(r);return dto(r);
 }
 @Transactional(readOnly=true)
 public byte[] imagem(Long admin,Long id) {
  admin(admin);var r=reports.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Denúncia indisponível."));
  if(r.getPost().getDeletedAt()!=null||!r.getPost().isPossuiImagem())throw error(HttpStatus.NOT_FOUND,"Imagem indisponível.");
  return images.findById(r.getPost().getId()).map(CommunityPostImage::getDados).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Imagem indisponível."));
 }
 private CommunityReportDTO dto(CommunityReport r) {
  var p=r.getPost();var c=r.getCommentId()==null?null:comments.findById(r.getCommentId()).orElse(null);
  boolean removed=p.getDeletedAt()!=null||(r.getCommentId()!=null&&(c==null||c.getDeletedAt()!=null));
  return new CommunityReportDTO(r.getId(),p.getCommunity().getId(),p.getCommunity().getHobby().getNome(),p.getId(),r.getCommentId(),
   r.getReason(),r.getDetalhes(),r.getStatus(),r.getDecision(),r.getNotas(),r.getCreatedAt(),r.getReviewedAt(),r.getVersion(),r.getTargetVersion(),
   c==null?p.getVersion():c.getVersion(),p.getConteudo(),c==null?null:c.getConteudo(),removed,
   p.getHiddenAt()!=null||(c!=null&&c.getHiddenAt()!=null),p.isPossuiImagem()&&p.getDeletedAt()==null);
 }
}
