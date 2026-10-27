import { test, afterEach } from "node:test";
import assert from "node:assert/strict";
import { session, revokeSession } from "./session.js";

const originalFetch=globalThis.fetch;
afterEach(() => { session.clear(); globalThis.fetch=originalFetch; });
test("session is held in memory and can be cleared", () => {
    session.setItem("token", "synthetic-token");
    assert.equal(session.getItem("token"),"synthetic-token");
    session.clear(); assert.equal(session.getItem("token"),null);
});
test("logout revokes the captured token on the server", async () => {
    session.setItem("token", "synthetic-token");
    globalThis.fetch=async (url, options) => {
        assert.equal(url,"/api/v1/auth/logout");
        assert.equal(options.headers.Authorization,"Bearer synthetic-token");
        assert.equal(options.method,"POST"); return {ok:true};
    };
    await revokeSession(); assert.equal(session.getItem("token"),null);
});
test("offline logout removes credentials locally", async () => {
    session.setItem("token", "synthetic-token");
    globalThis.fetch=async () => { throw new Error("offline"); };
    await revokeSession(); assert.equal(session.getItem("token"),null);
});
