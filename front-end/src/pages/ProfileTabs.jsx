import { Link, useSearchParams } from "react-router";
import MyProfile from "./MyProfile";
import SocialProfile from "./SocialProfile";
import "./ProfileTabs.css";

export default function ProfileTabs() {
    const [params] = useSearchParams();
    const social = params.get("aba") === "social";

    return (
        <>
            <nav className="profile-tabs" aria-label="Seções do perfil">
                <Link to="/myProfile" aria-current={social ? undefined : "page"}>Meu perfil</Link>
                <Link to="/myProfile?aba=social" aria-current={social ? "page" : undefined}>Meu perfil social</Link>
            </nav>
            {social ? <SocialProfile embedded /> : <MyProfile />}
        </>
    );
}
