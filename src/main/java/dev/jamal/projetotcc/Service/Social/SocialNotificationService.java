package dev.jamal.projetotcc.Service.Social;
import dev.jamal.projetotcc.DTO.Social.*;import dev.jamal.projetotcc.Repository.*;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;import org.springframework.http.HttpStatus;import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class SocialNotificationService {
 private final SocialNotificationStore store;private final UserRepository users;private final SocialProfileRepository profiles;
 private void validar(Long user){
  var nascimento=users.findBirthDateById(user).map(UserRepository.BirthDate::getDataNascimento).orElse(null);
  // A projeção vazia pode representar uma linha com data nula.
  if(nascimento==null&&!users.existsById(user))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  if(nascimento==null||nascimento.isAfter(LocalDate.now().minusYears(18))||!profiles.existsByUser_IdAndEnabledTrue(user))
   throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Ative sua participação social para acessar notificações.");
 }
 @Transactional(readOnly=true)
 public SocialNotificationCountDTO contar(Long user){validar(user);return new SocialNotificationCountDTO(store.contar(user));}
 @Transactional(readOnly=true)
 public SocialNotificationPageDTO listar(Long user,Long before,int limit){
  validar(user);if((before!=null&&before<=0)||limit<1||limit>30)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cursor inválido ou limite fora de 1 a 30.");
  var rows=store.listar(user,before==null?Long.MAX_VALUE:before,limit+1);var page=rows.stream().limit(limit).map(this::dto).toList();
  return new SocialNotificationPageDTO(page,rows.size()>limit,page.isEmpty()?null:page.get(page.size()-1).id(),store.contar(user));
 }
 @Transactional
 public void marcarLida(Long user,Long id){
  validar(user);if(id==null||id<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Notificação inválida.");
  if(store.marcarLida(user,id)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"A notificação não está mais disponível.");
 }
 private SocialNotificationDTO dto(SocialNotificationStore.Row r){
  String text,url;
  switch(r.tipo()){
   case SOLICITACAO_CONTATO ->{text=r.actor()+" enviou uma solicitação de contato.";url="/community?aba=conversas&caixa=RECEBIDAS";}
   case CONTATO_ACEITO ->{text=r.actor()+" aceitou sua solicitação de contato.";url="/community/conversations/"+r.contact();}
   case COMENTARIO_PUBLICACAO ->{text=r.actor()+" comentou na sua publicação.";url="/community/communities/"+r.community()+"?aba=posts&publicacao="+r.post()+"&comentario="+r.comment();}
   default ->throw new IllegalStateException("Tipo de notificação inválido.");
  }
  return new SocialNotificationDTO(r.id(),r.tipo(),text,url,r.created(),r.read()!=null,r.read());
 }
}
