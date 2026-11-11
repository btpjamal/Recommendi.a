export default function CommunityTabs({ active, onChange, disabled = false }) {
    return <nav className="community-post-tabs" aria-label="Seções da comunidade">
        <button type="button" aria-current={active === "posts" ? "page" : undefined}
            disabled={disabled} onClick={() => onChange("posts")}>Publicações</button>
        <button type="button" aria-current={active === "chat" ? "page" : undefined}
            disabled={disabled} onClick={() => onChange("chat")}>Chat</button>
    </nav>;
}
