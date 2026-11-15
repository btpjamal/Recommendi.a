CREATE INDEX idx_user_interest_user_interest ON user_interest (user_id, interest_id);
CREATE INDEX idx_user_hobbies_practicing_hobby_user ON user_hobbies (hobby_id, user_id)
    WHERE status_atual = 'PRATICANDO';
CREATE INDEX idx_recommendation_feedback_interested_hobby_user
    ON user_recommendation_feedbacks (hobby_id, user_id) WHERE tipo = 'INTERESSADO';
