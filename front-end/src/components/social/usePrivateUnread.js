import { useEffect, useState } from "react";
import { api } from "../../api/api";

export default function usePrivateUnread(enabled, revisao) {
    const [resumo, setResumo] = useState({ total: 0, porContato: [] });
    const [erro, setErro] = useState("");
    useEffect(() => {
        const controller = new AbortController();
        let timer;
        let ocupado = false;
        if (!enabled) return;
        async function carregar() {
            if (controller.signal.aborted || ocupado) return;
            clearTimeout(timer);
            if (document.hidden) { timer = setTimeout(carregar, 15000); return; }
            ocupado = true;
            let repetir = true;
            try {
                const resposta = await api.get("/community/private-messages/unread", { signal: controller.signal });
                if (!controller.signal.aborted) { setResumo(resposta.data); setErro(""); }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setResumo({ total: 0, porContato: [] });
                    setErro("Não foi possível atualizar a contagem de mensagens não lidas.");
                }
                if ([401, 403].includes(error.response?.status)) repetir = false;
            } finally {
                ocupado = false;
                if (!controller.signal.aborted && repetir) timer = setTimeout(carregar, 15000);
            }
        }
        function aoVoltar() { if (!document.hidden) carregar(); }
        document.addEventListener("visibilitychange", aoVoltar);
        carregar();
        return () => { clearTimeout(timer); controller.abort(); document.removeEventListener("visibilitychange", aoVoltar); };
    }, [enabled, revisao]);
    return enabled ? { ...resumo, erro } : { total: 0, porContato: [], erro: "" };
}
