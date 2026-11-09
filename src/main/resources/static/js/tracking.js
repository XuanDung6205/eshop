(() => {
    const page = document.body.dataset.trackPage;
    const forms = document.querySelectorAll("form[data-track-event], form[data-track-session]");
    if (!page && forms.length === 0) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    const endpoint = document.querySelector('meta[name="_tracking_endpoint"]')?.content;
    if (!csrfToken || !csrfHeader || !endpoint) return;

    let sessionId;
    try {
        sessionId = sessionStorage.getItem("eshop.trackingSessionId");
        if (!sessionId) {
            sessionId = crypto.randomUUID();
            sessionStorage.setItem("eshop.trackingSessionId", sessionId);
        }
    } catch (_) {
        sessionId = crypto.randomUUID();
    }

    const positiveId = value => {
        const number = Number(value);
        return Number.isSafeInteger(number) && number > 0 ? number : undefined;
    };
    const nonnegativeNumber = value => {
        const number = Number(value);
        return Number.isSafeInteger(number) && number >= 0 ? number : undefined;
    };
    const pagePath = window.location.pathname;

    function send(eventType, details = {}) {
        fetch(endpoint, {
            method: "POST",
            credentials: "same-origin",
            keepalive: true,
            headers: {
                "Content-Type": "application/json",
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                eventType,
                sessionId,
                occurredAt: new Date().toISOString(),
                pagePath,
                ...details
            })
        }).catch(() => {});
    }

    if (page) {
        send("PAGE_VIEW");
    }
    if (page === "product-detail") {
        const productId = positiveId(document.body.dataset.trackProductId);
        if (productId) send("PRODUCT_VIEW", {productId});
    }
    if (page === "products") {
        const keyword = document.body.dataset.trackKeyword?.trim().slice(0, 200);
        const categoryId = positiveId(document.body.dataset.trackCategoryId);
        const resultCount = nonnegativeNumber(document.body.dataset.trackResultCount);
        if (keyword) send("SEARCH", {keyword, categoryId, resultCount});
        if (categoryId) send("FILTER", {keyword, categoryId, resultCount});
    }
    if (page === "checkout") {
        send("CHECKOUT_START");
    }

    for (const form of forms) {
        let sessionField = form.querySelector('input[name="trackingSessionId"]');
        if (!sessionField) {
            sessionField = document.createElement("input");
            sessionField.type = "hidden";
            sessionField.name = "trackingSessionId";
            form.appendChild(sessionField);
        }
        sessionField.value = sessionId;

        if (!form.dataset.trackEvent) continue;
        form.addEventListener("submit", () => {
            const productId = positiveId(form.querySelector('input[name="productId"]')?.value
                ?? form.dataset.trackProductId);
            const quantity = nonnegativeNumber(form.querySelector('input[name="quantity"]')?.value ?? "1");
            send(form.dataset.trackEvent, {productId, quantity});
        });
    }
})();
