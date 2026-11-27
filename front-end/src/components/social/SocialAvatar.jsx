
import {
    useEffect,
    useId,
    useRef,
    useState,
    useSyncExternalStore
} from "react";
import {
    session,
    subscribeSessionToken
} from "../../auth/session.js";
import "./SocialAvatar.css";
import {
    solicitarMiniatura,
    solicitarFoto,
    subscribeMiniaturas,
    getThumbnailRevision
} from "./socialAvatarRequest.js";

function ExpandablePhoto({
                             nome,
                             userId,
                             token,
                             thumbnail
                         }) {

    const dialog = useRef(null);
    const titleId = useId();

    const request = useRef(null);
    const objectUrl = useRef(null);

    const [fullImage, setFullImage] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(false);

    function limparFoto() {

        request.current?.liberar();
        request.current = null;

        if (objectUrl.current) {
            URL.revokeObjectURL(objectUrl.current);
            objectUrl.current = null;
        }
    }

    useEffect(() => {

        return () => {
            limparFoto();
        };

    }, []);

    async function abrirFoto() {

        limparFoto();

        setFullImage(null);
        setError(false);
        setLoading(true);

        dialog.current.showModal();

        const operacao = solicitarFoto(userId, token);
        request.current = operacao;
        const ativa = () => request.current === operacao && session.getItem("token") === token;

        try {
            const imagem = await operacao.promise;
            if (!ativa()) return;

            objectUrl.current = URL.createObjectURL(imagem);
            setFullImage(objectUrl.current);
        } catch {
            if (ativa()) setError(true);
        } finally {
            operacao.liberar();
            if (ativa()) setLoading(false);
        }
    }

    function aoFechar() {

        limparFoto();

        setFullImage(null);
        setLoading(false);
        setError(false);
    }

    return (
        <>
            <button
                type="button"
                className="social-user-avatar social-avatar-trigger"
                aria-label={`Ampliar foto de ${nome}`}
                aria-haspopup="dialog"
                onClick={abrirFoto}
            >
                {thumbnail}
            </button>

            <dialog
                ref={dialog}
                className="social-avatar-dialog"
                aria-labelledby={titleId}
                onClose={aoFechar}
                onCancel={event => event.stopPropagation()}
            >

                <header>

                    <h2 id={titleId}>
                        Foto de {nome}
                    </h2>

                    <button
                        type="button"
                        className="community-secondary-button"
                        onClick={() => dialog.current.close()}
                    >
                        Fechar
                    </button>

                </header>

                {loading && (
                    <p role="status">
                        Carregando fotografia...
                    </p>
                )}

                {error && (
                    <p role="alert">
                        Fotografia indisponível.
                    </p>
                )}

                {fullImage && (
                    <img
                        src={fullImage}
                        alt={`Foto de ${nome}`}
                    />
                )}

            </dialog>
        </>
    );
}

export default function SocialAvatar({
                                         usuario,
                                         revision = 0,
                                         ampliavel = false
                                     }) {

    const [image, setImage] = useState(null);

    const token = useSyncExternalStore(
        subscribeSessionToken,
        () => session.getItem("token")
    );

    const thumbnailRevision = useSyncExternalStore(subscribeMiniaturas, getThumbnailRevision);

    useEffect(() => {

        setImage(null);

        if (!usuario?.possuiFoto || !token) return;

        let ativo = true;
        let url;

        const solicitacao = solicitarMiniatura(
            usuario.userId,
            token
        );

        solicitacao.promise.then(data => {

            if (
                !ativo ||
                session.getItem("token") !== token ||
                getThumbnailRevision() !== thumbnailRevision
            ) return;

            url = URL.createObjectURL(data);

            setImage({
                url,
                id: usuario.userId,
                revision,
                thumbnailRevision,
                token
            });

        }).catch(() => {

            if (ativo) {
                setImage(null);
            }

        });

        return () => {

            ativo = false;

            solicitacao.liberar();

            if (url) {
                URL.revokeObjectURL(url);
            }
        };

    }, [
        usuario?.userId,
        usuario?.possuiFoto,
        revision,
        thumbnailRevision,
        token
    ]);

    const src =
        usuario?.possuiFoto &&
        image?.id === usuario.userId &&
        image.revision === revision &&
        image.thumbnailRevision === thumbnailRevision &&
        image.token === token
            ? image.url
            : null;

    const thumbnail = src && (
        <img
            src={src}
            alt={`Foto de ${usuario.nome}`}
            style={{
                width: "100%",
                height: "100%",
                objectFit: "cover",
                borderRadius: "inherit"
            }}
        />
    );

    if (ampliavel && src) {

        return (
            <ExpandablePhoto
                key={src}
                nome={usuario.nome}
                userId={usuario.userId}
                token={token}
                thumbnail={thumbnail}
            />
        );
    }

    return (
        <div className="social-user-avatar">
            {thumbnail ||
                usuario?.nome?.charAt(0)?.toUpperCase()}
        </div>
    );
}
