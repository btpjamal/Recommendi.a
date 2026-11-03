import ReactMarkdown from "react-markdown";

const allowed = ["p", "br", "hr", "h1", "h2", "h3", "h4", "h5", "h6",
    "strong", "em", "ul", "ol", "li", "blockquote", "pre", "code", "del"];

export default function SafeMarkdown({ children }) {
    return <ReactMarkdown skipHtml allowedElements={allowed} unwrapDisallowed>
        {children}
    </ReactMarkdown>;
}
