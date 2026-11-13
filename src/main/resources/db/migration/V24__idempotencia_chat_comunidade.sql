ALTER TABLE community_messages ADD COLUMN client_message_id uuid;

CREATE UNIQUE INDEX uq_community_messages_client_message
    ON community_messages (community_id, user_id, client_message_id)
    WHERE client_message_id IS NOT NULL;
