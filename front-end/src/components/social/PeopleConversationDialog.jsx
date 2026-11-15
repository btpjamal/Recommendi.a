import { useEffect, useRef } from "react";
import "./PeopleConversationDialog.css";

export default function PeopleConversationDialog({ children, onClose }) {
    const ref = useRef(null);

    useEffect(() => {
        const dialog = ref.current;
        const previousFocus = document.activeElement;
        dialog.showModal();
        dialog.querySelector("input[type=search]").focus();
        return () => {
            dialog.close();
            if (previousFocus?.isConnected) previousFocus.focus();
        };
    }, []);

    return (
        <dialog ref={ref} className="people-conversation-dialog" aria-labelledby="people-dialog-title"
            onCancel={event => { event.preventDefault(); onClose(); }}>
            <header className="people-dialog-header">
                <h2 id="people-dialog-title">Nova conversa</h2>
                <button type="button" className="community-secondary-button" onClick={onClose}>Fechar</button>
            </header>
            {children}
        </dialog>
    );
}
