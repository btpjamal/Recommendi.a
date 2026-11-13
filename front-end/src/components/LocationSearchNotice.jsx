import { useEffect, useSyncExternalStore } from "react";
import { Link, useLocation } from "react-router";
import {
    dismissLocationSearchNotice,
    getLocationSearchNotice,
    subscribeLocationSearch,
} from "./hobbyLocationSearch.js";
import "./LocationSearchNotice.css";

export default function LocationSearchNotice() {
    const notice = useSyncExternalStore(subscribeLocationSearch, getLocationSearchNotice);
    const { pathname, hash } = useLocation();

    useEffect(() => {
        if (hash !== "#locais-proximos" || !/^\/recommendations\/\d+\/?$/i.test(pathname)) return;
        function showLocations() {
            const section = document.getElementById("locais-proximos");
            if (!section) return false;
            section.scrollIntoView({ block: "start" });
            return true;
        }
        if (showLocations()) return;
        const observer = new MutationObserver(() => {
            if (showLocations()) observer.disconnect();
        });
        observer.observe(document.getElementById("root"), { childList: true, subtree: true });
        return () => observer.disconnect();
    }, [hash, pathname]);

    const destination = notice && `/recommendations/${notice.hobbyId}`;
    const onDetails = pathname.toLowerCase().replace(/\/$/, "") === destination;

    return (
        <aside className="location-search-notice" hidden={!notice || onDetails}>
            <div role="status">
                {notice && !onDetails && (
                    <>
                        <strong>Busca de locais · {notice.hobbyNome}</strong>
                        <p>{notice.message}</p>
                    </>
                )}
            </div>
            {notice && (
                <div className="location-search-notice-actions">
                    <Link to={`${destination}#locais-proximos`} onClick={dismissLocationSearchNotice}>
                        Ver busca
                    </Link>
                    <button type="button" onClick={dismissLocationSearchNotice}>Fechar</button>
                </div>
            )}
        </aside>
    );
}
