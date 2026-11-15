CREATE TABLE community_report_evidence (
    report_id bigint PRIMARY KEY REFERENCES community_reports(id) ON DELETE CASCADE,
    post_text varchar(4000) NOT NULL,
    comment_text varchar(1000),
    image_data bytea,
    sha256 varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL
);

CREATE INDEX idx_community_report_evidence_expiry ON community_report_evidence(expires_at);
