import { useEffect } from "react";
import { useLocation } from "react-router";

export default function LocationSearchAnchor() {
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

    return null;
}
