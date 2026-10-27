-- Account deletion removes owned data and messages, not shared catalog or communities.
ALTER TABLE user_hobbies DROP CONSTRAINT fk1aia57pgihndujoa8vm7eoccn,
    ADD CONSTRAINT fk1aia57pgihndujoa8vm7eoccn FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE community_messages DROP CONSTRAINT fk7959yqt4apnarry6pdsyjctjm,
    ADD CONSTRAINT fk7959yqt4apnarry6pdsyjctjm FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE recommendation_profile DROP CONSTRAINT fk8msyei0fxwa2lt3xvwjie9bxt,
    ADD CONSTRAINT fk8msyei0fxwa2lt3xvwjie9bxt FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_interest DROP CONSTRAINT fk97yodomof0ffqwpnt9obp1l6c,
    ADD CONSTRAINT fk97yodomof0ffqwpnt9obp1l6c FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_objectives DROP CONSTRAINT fk9rycpqamoy0hec9b5foigvx6d,
    ADD CONSTRAINT fk9rycpqamoy0hec9b5foigvx6d FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_recommendation_feedbacks DROP CONSTRAINT fkajvohpdipok1v7gdq0tjp6w3a,
    ADD CONSTRAINT fkajvohpdipok1v7gdq0tjp6w3a FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_ai_configurations DROP CONSTRAINT fkb8gwkjygbagd7qhchtnuldsfe,
    ADD CONSTRAINT fkb8gwkjygbagd7qhchtnuldsfe FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE personalized_plans DROP CONSTRAINT fke10ebgwjc0v9hm5baayvp0ad2,
    ADD CONSTRAINT fke10ebgwjc0v9hm5baayvp0ad2 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE social_profiles DROP CONSTRAINT fkegvcj1tq53c5diyxyytp816rl,
    ADD CONSTRAINT fkegvcj1tq53c5diyxyytp816rl FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE hobby_suggestion DROP CONSTRAINT fkhakyyqmn6e7ld2cypiv37qw1r,
    ADD CONSTRAINT fkhakyyqmn6e7ld2cypiv37qw1r FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_hobby_feedback DROP CONSTRAINT fkj220sp597ic5to7hytydhe3yc,
    ADD CONSTRAINT fkj220sp597ic5to7hytydhe3yc FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE hobby_suggestion DROP CONSTRAINT fklnbp2omi22hxbywjl38jkhqf1,
    ADD CONSTRAINT fklnbp2omi22hxbywjl38jkhqf1 FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE community_members DROP CONSTRAINT fkme7k1stbnwi6cpmm8a6sgcikn,
    ADD CONSTRAINT fkme7k1stbnwi6cpmm8a6sgcikn FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE general_personalized_plan DROP CONSTRAINT fknhaj0ovp0uhlsw8qndg4cuefk,
    ADD CONSTRAINT fknhaj0ovp0uhlsw8qndg4cuefk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
