(() => {
    const root = document.getElementById("eshop-ai-chat");
    if (!root) return;

    const launcher = root.querySelector(".eshop-chat__launcher");
    const panel = root.querySelector(".eshop-chat__panel");
    const closeButton = root.querySelector(".eshop-chat__close");
    const messages = root.querySelector(".eshop-chat__messages");
    const form = root.querySelector(".eshop-chat__form");
    const input = root.querySelector("#eshop-chat-input");
    const sendButton = root.querySelector(".eshop-chat__send");
    const status = root.querySelector(".eshop-chat__status");
    const endpoint = root.dataset.chatEndpoint;
    let sessionId = getSessionId();
    let inFlight = false;
    const history = [];

    function getSessionId() {
        try {
            let value = sessionStorage.getItem("eshop.trackingSessionId");
            if (!value) {
                value = crypto.randomUUID();
                sessionStorage.setItem("eshop.trackingSessionId", value);
            }
            return value;
        } catch (_) {
            return crypto.randomUUID();
        }
    }

    function setOpen(open) {
        panel.hidden = !open;
        panel.setAttribute("aria-hidden", String(!open));
        launcher.setAttribute("aria-expanded", String(open));
        launcher.setAttribute("aria-label", open ? "Đóng tư vấn AI" : "Mở tư vấn AI");
        if (open) input.focus();
        else launcher.focus();
    }

    function addMessage(text, kind, sources = []) {
        const bubble = document.createElement("article");
        bubble.className = `eshop-chat__message eshop-chat__message--${kind}`;
        bubble.textContent = text;

        const validSources = Array.isArray(sources) ? sources.filter(source =>
            source && typeof source.name === "string" && typeof source.url === "string"
            && /^\/products\/\d+$/.test(source.url)) : [];
        if (kind === "assistant" && validSources.length) {
            const list = document.createElement("div");
            list.className = "eshop-chat__sources";
            for (const source of validSources) {
                const link = document.createElement("a");
                link.className = "eshop-chat__source";
                link.href = source.url;
                const name = document.createElement("span");
                name.className = "eshop-chat__source-name";
                name.textContent = source.name;
                const meta = document.createElement("span");
                meta.className = "eshop-chat__source-meta";
                const price = source.price == null ? Number.NaN : Number(source.price);
                const stock = source.quantity == null ? Number.NaN : Number(source.quantity);
                meta.textContent = `${Number.isFinite(price) ? new Intl.NumberFormat("vi-VN").format(price) + " ₫" : "Giá liên hệ"}`
                    + ` · ${Number.isFinite(stock) ? `Còn ${stock}` : "Chưa rõ tồn kho"}`;
                link.append(name, meta);
                list.appendChild(link);
            }
            bubble.appendChild(list);
        }

        messages.appendChild(bubble);
        messages.scrollTop = messages.scrollHeight;
        return bubble;
    }

    async function sendMessage(text) {
        const question = text.trim();
        if (!question || inFlight) return;

        inFlight = true;
        status.textContent = "";
        sendButton.disabled = true;
        input.disabled = true;
        addMessage(question, "user");
        const pending = addMessage("Đang tìm sản phẩm phù hợp…", "assistant");
        pending.classList.add("eshop-chat__message--pending");

        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({message: question, sessionId, history: history.slice(-8)})
            });
            if (!response.ok) throw new Error("chat_unavailable");
            const data = await response.json();
            pending.remove();
            if (typeof data.reply !== "string" || !data.reply.trim()) {
                throw new Error("empty_reply");
            }
            addMessage(data.reply, "assistant", data.sources);
            history.push({role: "user", content: question}, {role: "assistant", content: data.reply});
            if (history.length > 8) history.splice(0, history.length - 8);
        } catch (_) {
            pending.remove();
            status.textContent = "Chưa kết nối được trợ lý. Bạn thử lại sau nhé.";
        } finally {
            inFlight = false;
            sendButton.disabled = false;
            input.disabled = false;
            input.focus();
        }
    }

    launcher.addEventListener("click", () => setOpen(panel.hidden));
    closeButton.addEventListener("click", () => setOpen(false));
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !panel.hidden) setOpen(false);
    });

    form.addEventListener("submit", event => {
        event.preventDefault();
        const question = input.value;
        if (!question.trim()) return;
        input.value = "";
        input.style.height = "auto";
        sendMessage(question);
    });

    input.addEventListener("input", () => {
        input.style.height = "auto";
        input.style.height = `${Math.min(input.scrollHeight, 110)}px`;
    });
    input.addEventListener("keydown", event => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    root.querySelectorAll("[data-chat-prompt]").forEach(button => {
        button.addEventListener("click", () => {
            input.value = button.dataset.chatPrompt;
            form.requestSubmit();
        });
    });
})();
