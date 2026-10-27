-- PostgreSQL 15+. Merge duplicates before enforcing NULL-aware uniqueness.
LOCK TABLE communities, community_members, community_messages IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE community_merge_v7 ON COMMIT DROP AS
SELECT id AS old_id, min(id) OVER (PARTITION BY hobby_id, tipo, estado, cidade) AS keep_id
FROM communities;

UPDATE community_messages m SET community_id = c.keep_id
FROM community_merge_v7 c WHERE m.community_id = c.old_id AND c.old_id <> c.keep_id;

DELETE FROM community_members WHERE id IN (
    SELECT id FROM (
        SELECT m.id, row_number() OVER (
            PARTITION BY c.keep_id, m.user_id ORDER BY m.data_entrada, m.id
        ) AS occurrence
        FROM community_members m JOIN community_merge_v7 c ON c.old_id = m.community_id
    ) ranked WHERE occurrence > 1
);

UPDATE community_members m SET community_id = c.keep_id
FROM community_merge_v7 c WHERE m.community_id = c.old_id AND c.old_id <> c.keep_id;

DELETE FROM communities WHERE id IN (
    SELECT old_id FROM community_merge_v7 WHERE old_id <> keep_id
);

CREATE UNIQUE INDEX ux_community_location_nulls
ON communities (hobby_id, tipo, estado, cidade) NULLS NOT DISTINCT;
