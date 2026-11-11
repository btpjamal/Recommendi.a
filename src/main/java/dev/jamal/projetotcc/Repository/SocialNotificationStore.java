package dev.jamal.projetotcc.Repository;
import dev.jamal.projetotcc.Enum.SocialNotificationType;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import lombok.RequiredArgsConstructor;
import java.time.*;import java.util.*;

@Repository @RequiredArgsConstructor
public class SocialNotificationStore {
 private final NamedParameterJdbcTemplate jdbc;
 public record Row(Long id,SocialNotificationType tipo,String actor,Long contact,Long comment,Long post,Long community,Instant created,Instant read) {}
 // The same predicate gates the list, count and read marker, including recycled contact requests.
 private static final String VISIBLE="""
  FROM social_notifications n
  JOIN users a ON a.id=n.actor_id JOIN users r ON r.id=n.recipient_id
  JOIN social_profiles sa ON sa.user_id=a.id AND sa.enabled=true
  JOIN social_profiles sr ON sr.user_id=r.id AND sr.enabled=true
  LEFT JOIN social_contacts c ON c.id=n.contact_id
  LEFT JOIN community_comments cm ON cm.id=n.comment_id
  LEFT JOIN community_posts p ON p.id=cm.post_id
  WHERE n.recipient_id=:recipient AND a.data_nascimento<=:cutoff AND r.data_nascimento<=:cutoff
   AND NOT EXISTS(SELECT 1 FROM social_user_blocks b WHERE
    (b.blocker_id=n.actor_id AND b.blocked_id=n.recipient_id) OR (b.blocker_id=n.recipient_id AND b.blocked_id=n.actor_id))
   AND ((n.tipo='SOLICITACAO_CONTATO' AND c.notification_cycle=n.contact_cycle AND c.status='PENDENTE'
       AND c.requester_id=n.actor_id AND c.recipient_id=n.recipient_id)
    OR (n.tipo='CONTATO_ACEITO' AND c.notification_cycle=n.contact_cycle AND c.status='ACEITA'
       AND c.recipient_id=n.actor_id AND c.requester_id=n.recipient_id)
    OR (n.tipo='COMENTARIO_PUBLICACAO' AND cm.user_id=n.actor_id AND p.user_id=n.recipient_id
       AND cm.deleted_at IS NULL AND cm.hidden_at IS NULL AND p.deleted_at IS NULL AND p.hidden_at IS NULL
       AND EXISTS(SELECT 1 FROM community_members m WHERE m.community_id=p.community_id AND m.user_id=n.recipient_id)))
  """;
 private Map<String,Object> parameters(Long recipient){var p=new HashMap<String,Object>();p.put("recipient",recipient);p.put("cutoff",java.sql.Date.valueOf(LocalDate.now().minusYears(18)));return p;}
 public List<Row> listar(Long recipient,Long before,int limit){
  var p=parameters(recipient);p.put("before",before);p.put("limit",limit);
  return jdbc.query("SELECT n.id,n.tipo,a.nome,n.contact_id,n.comment_id,cm.post_id,p.community_id,n.created_at,n.read_at "+VISIBLE+" AND n.id<:before ORDER BY n.id DESC LIMIT :limit",p,
   (rs,i)->new Row(rs.getLong("id"),SocialNotificationType.valueOf(rs.getString("tipo")),rs.getString("nome"),
    rs.getObject("contact_id",Long.class),rs.getObject("comment_id",Long.class),rs.getObject("post_id",Long.class),rs.getObject("community_id",Long.class),
    rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("read_at")==null?null:rs.getTimestamp("read_at").toInstant()));
 }
 public long contar(Long recipient){Long count=jdbc.queryForObject("SELECT count(*) "+VISIBLE+" AND n.read_at IS NULL",parameters(recipient),Long.class);return count==null?0:count;}
 public int marcarLida(Long recipient,Long id){
  var p=parameters(recipient);p.put("id",id);
  return jdbc.update("UPDATE social_notifications SET read_at=coalesce(read_at,clock_timestamp()) WHERE id=:id AND id IN(SELECT n.id "+VISIBLE+" AND n.id=:id)",p);
 }
 public void registrar(SocialNotificationType type,Long source,UUID cycle){
  var p=new HashMap<String,Object>();p.put("source",source);p.put("cycle",cycle);p.put("type",type.name());
  String sourceSql,columns,selection,condition;
  if(type==SocialNotificationType.COMENTARIO_PUBLICACAO){
   sourceSql=" FROM community_comments c JOIN community_posts p ON p.id=c.post_id WHERE c.id=:source AND c.user_id<>p.user_id";
   columns="recipient_id,actor_id,tipo,comment_id";selection="p.user_id,c.user_id,:type,c.id";condition="p.user_id";
  }else{
   boolean request=type==SocialNotificationType.SOLICITACAO_CONTATO;
   sourceSql=" FROM social_contacts c WHERE c.id=:source AND c.notification_cycle=:cycle AND c.status='"+(request?"PENDENTE":"ACEITA")+"'";
   columns="recipient_id,actor_id,tipo,contact_id,contact_cycle";
   selection=(request?"c.recipient_id,c.requester_id":"c.requester_id,c.recipient_id")+",:type,c.id,c.notification_cycle";
   condition=request?"c.recipient_id":"c.requester_id";
  }
  var recipients=jdbc.queryForList("SELECT "+condition+sourceSql,p,Long.class);
  if(recipients.isEmpty())return;
  p.put("key","recommendi.notifications."+recipients.get(0));
  // Serialize only this inbox, without taking a lock on another user's row after a community lock.
  jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(cast(:key as text),0))",p,(ResultSetExtractor<Void>)rs->null);
  jdbc.update("INSERT INTO social_notifications ("+columns+") SELECT "+selection+sourceSql+" ON CONFLICT DO NOTHING",p);
 }
}
