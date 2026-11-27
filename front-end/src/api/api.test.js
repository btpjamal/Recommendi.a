import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import axios from "axios";
import { api } from "./api.js";
import { revokeSession, session } from "../auth/session.js";

const redirects = [];
const originalFetch = globalThis.fetch;
globalThis.window = {
    location: { origin: "http://localhost", pathname: "/recommendations", assign: path => redirects.push(path) }
};

afterEach(() => {
    session.clear();
    redirects.length = 0;
    window.location.pathname = "/recommendations";
    globalThis.fetch = originalFetch;
});

function signIn(account) {
    session.start({ token: `test-${account}`, userId: account });
}

function pending({ method = "get", url = "/private", signal, headers } = {}) {
    let finish;
    let markSent;
    const sent = new Promise(resolve => { markSent = resolve; });
    const result = api.request({
        method, url, signal, headers,
        adapter: config => new Promise((resolve, reject) => {
            finish = status => {
                const response = { config, status, data: { sample: true }, headers: {} };
                if (status < 400) resolve(response);
                else reject(new axios.AxiosError("HTTP error", "ERR_BAD_REQUEST", config, null, response));
            };
            markSent(config.headers.toJSON());
        })
    }).then(response => response, error => error);
    return { sent, result, release: async status => { await sent; finish(status); return result; } };
}

for (const account of ["B", "A"]) {
    test(`old authenticated 401 cannot clear the new session (${account})`, async () => {
        signIn("A");
        const request = pending();
        assert.deepEqual(Object.keys(await request.sent).sort(), ["Accept", "Authorization"]);
        assert.equal((await request.sent).Authorization, "Bearer test-A");
        session.clear();
        signIn(account);
        assert.equal(axios.isCancel(await request.release(401)), true);
        assert.equal(session.getItem("userId"), account);
        assert.equal(session.getItem("token"), `test-${account}`);
        assert.deepEqual(redirects, []);
    });
}

test("concurrent current-session 401 clears credentials and redirects once", async () => {
    signIn("A");
    const first = pending();
    const second = pending();
    await Promise.all([first.sent, second.sent]);
    const [expired, duplicate] = await Promise.all([first.release(401), second.release(401)]);
    assert.equal(expired.response.status, 401);
    assert.equal(axios.isCancel(duplicate), true);
    assert.equal(session.getItem("token"), null);
    assert.equal(session.getItem("userId"), null);
    assert.deepEqual(redirects, ["/login"]);
});

test("current 401 on the login page clears credentials without reloading", async () => {
    window.location.pathname = "/login";
    signIn("A");
    assert.equal((await pending().release(401)).response.status, 401);
    assert.equal(session.getItem("token"), null);
    assert.deepEqual(redirects, []);
});

test("anonymous login stays anonymous and a delayed failure cannot invalidate login", async () => {
    const request = pending({ method: "post", url: "/auth/login" });
    assert.equal((await request.sent).Authorization, undefined);
    signIn("B");
    assert.equal(axios.isCancel(await request.release(401)), true);
    const publicRequest = pending({ method: "post", url: "/auth/login" });
    assert.equal((await publicRequest.sent).Authorization, undefined);
    assert.equal((await publicRequest.release(401)).response.status, 401);
    assert.equal(session.getItem("userId"), "B");
    assert.deepEqual(redirects, []);
});

test("403 and canceled requests preserve the current session", async () => {
    signIn("A");
    assert.equal((await pending().release(403)).response.status, 403);
    const controller = new AbortController();
    const request = pending({ signal: controller.signal });
    await request.sent;
    controller.abort();
    assert.equal(axios.isCancel(await request.release(401)), true);
    assert.equal(session.getItem("userId"), "A");
    assert.deepEqual(redirects, []);
});

test("a supplied credential from another session cannot be sent as the active account", async () => {
    signIn("B");
    const request = pending({ headers: { Authorization: "Bearer test-A" } });
    assert.equal(axios.isCancel(await request.result), true);
    assert.equal(session.getItem("userId"), "B");
});

test("late successes and errors are discarded without exposing data to consumers", async () => {
    signIn("A");
    const success = pending();
    const failure = pending();
    await Promise.all([success.sent, failure.sent]);
    signIn("A");
    for (const error of await Promise.all([success.release(200), failure.release(500)])) {
        assert.equal(axios.isCancel(error), true);
        assert.equal(error.response, undefined);
        assert.equal(error.config, undefined);
    }
    assert.deepEqual((await pending().release(200)).data, { sample: true });
});

test("late anonymous login success cannot replace the current session", async () => {
    const login = pending({ method: "post", url: "/auth/login" });
    await login.sent;
    signIn("B");
    assert.equal(axios.isCancel(await login.release(200)), true);
    assert.equal(session.getItem("userId"), "B");
});

test("late logout completion cannot navigate a newly authenticated session", async () => {
    signIn("A");
    let finish;
    globalThis.fetch = () => new Promise(resolve => { finish = resolve; });
    const logout = revokeSession(() => redirects.push("/login"));
    assert.equal(session.getItem("token"), null);
    signIn("B");
    finish({ ok: true });
    await logout;
    assert.equal(session.getItem("userId"), "B");
    assert.deepEqual(redirects, []);
});
