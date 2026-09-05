const state = {
  token: sessionStorage.getItem("aiShoppingToken"),
  customerId: sessionStorage.getItem("aiShoppingCustomerId"),
  conversationId: sessionStorage.getItem("aiShoppingConversationId") || crypto.randomUUID(),
  products: [],
  categories: [],
  wishes: [],
  profile: null,
  authMode: "login"
};

sessionStorage.setItem("aiShoppingConversationId", state.conversationId);

const $ = (selector) => document.querySelector(selector);
const authView = $("#authView");
const appShell = $("#appShell");
const productGrid = $("#productGrid");
const chatMessages = $("#chatMessages");

async function api(path, options = {}) {
  const headers = { ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const response = await fetch(path, { ...options, headers });
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : await response.text();
  if (!response.ok) {
    if (response.status === 401 && state.token) logout();
    const error = new Error(body?.message || body?.code || body || "요청을 처리하지 못했습니다.");
    error.status = response.status;
    error.code = body?.code;
    throw error;
  }
  return body;
}

function setAuthMode(mode) {
  state.authMode = mode;
  const login = mode === "login";
  $("#loginTab").classList.toggle("active", login);
  $("#registerTab").classList.toggle("active", !login);
  $("#referrerField").classList.toggle("hidden", login);
  $("#authTitle").textContent = login ? "다시 만나 반가워요" : "새로운 쇼핑을 시작해요";
  $("#authSubtitle").textContent = login ? "쇼핑을 계속하려면 로그인하세요." : "간단한 정보만으로 바로 시작할 수 있어요.";
  $("#authSubmit").textContent = login ? "로그인" : "회원가입";
  $("#password").autocomplete = login ? "current-password" : "new-password";
  $("#authError").textContent = "";
}

async function handleAuth(event) {
  event.preventDefault();
  const submit = $("#authSubmit");
  const customerId = $("#customerId").value.trim();
  const customerPassword = $("#password").value;
  submit.disabled = true;
  $("#authError").textContent = "";
  try {
    if (state.authMode === "register") {
      const referrerId = $("#referrerId").value.trim();
      await api("/api/customers", {
        method: "POST",
        body: JSON.stringify({ customerId, customerPassword, referrerId: referrerId || null })
      });
    }
    const login = await api("/api/customers/login", {
      method: "POST",
      body: JSON.stringify({ customerId, customerPassword })
    });
    state.token = login.accessToken;
    state.customerId = customerId;
    sessionStorage.setItem("aiShoppingToken", state.token);
    sessionStorage.setItem("aiShoppingCustomerId", customerId);
    await enterApp();
  } catch (error) {
    $("#authError").textContent = readableError(error);
  } finally {
    submit.disabled = false;
  }
}

async function enterApp() {
  authView.classList.add("hidden");
  appShell.classList.remove("hidden");
  $("#customerName").textContent = state.customerId?.toUpperCase() || "YOU";
  try {
    await Promise.all([loadProducts(), loadAccount()]);
  } catch (error) {
    toast(readableError(error));
  }
  await checkAiAvailability();
}

function logout() {
  state.token = null;
  state.customerId = null;
  sessionStorage.removeItem("aiShoppingToken");
  sessionStorage.removeItem("aiShoppingCustomerId");
  appShell.classList.add("hidden");
  authView.classList.remove("hidden");
  setAuthMode("login");
}

async function loadProducts(filters = {}) {
  productGrid.innerHTML = '<div class="empty-state">상품을 불러오는 중입니다…</div>';
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value); });
  state.products = await api(`/api/products${params.size ? `/search?${params}` : ""}`);
  if (!params.size) state.categories = [...new Set(state.products.map((p) => p.category).filter(Boolean))];
  updateCategories();
  renderProducts();
}

function updateCategories() {
  const select = $("#categorySelect");
  const selected = select.value;
  const categories = state.categories;
  select.replaceChildren(new Option("모든 카테고리", ""), ...categories.map((value) => new Option(value, value)));
  if (categories.includes(selected)) select.value = selected;
}

function renderProducts() {
  productGrid.replaceChildren();
  if (!state.products.length) {
    productGrid.append(empty("조건에 맞는 상품이 없습니다."));
    return;
  }
  state.products.forEach((product) => {
    const card = element("article", "product-card");
    const visual = element("div", "product-visual", initials(product.productName));
    const meta = element("div", "product-meta");
    meta.append(element("span", "", product.category || "상품"), element("span", "", `재고 ${product.stockQuantity}`));
    const title = element("h3", "", product.productName);
    const description = element("p", "", product.description || "상품 설명을 준비하고 있습니다.");
    const bottom = element("div", "product-bottom");
    const price = element("strong", "product-price", `${formatNumber(product.productPrice)} P`);
    const actions = element("div", "card-actions");
    const wished = state.wishes.some((wish) => wish.productId === product.id);
    const wishButton = element("button", "small-button", wished ? "찜 해제" : "♡ 찜");
    wishButton.type = "button";
    wishButton.addEventListener("click", () => toggleWish(product, wished));
    const orderButton = element("button", "small-button accent", "주문");
    orderButton.type = "button";
    orderButton.disabled = product.stockQuantity < 1;
    orderButton.addEventListener("click", () => orderProduct(product));
    actions.append(wishButton, orderButton);
    bottom.append(price, actions);
    card.append(visual, meta, title, description, bottom);
    productGrid.append(card);
  });
}

async function loadAccount() {
  const [profile, wishes] = await Promise.all([api("/api/customers/me"), api("/api/wishes")]);
  state.profile = profile;
  state.wishes = wishes;
  $("#pointValue").textContent = `${formatNumber(profile.customerPoint)} P`;
  $("#wishCount").textContent = wishes.length;
  $("#orderCount").textContent = profile.products.length;
  renderMiniList($("#wishList"), wishes.map((item) => ({ name: item.productName, detail: `${formatNumber(item.productPrice)} P` })));
  renderMiniList($("#orderList"), profile.products.map((item) => ({
    name: item.productName,
    detail: `${item.quantity}개`,
    actionLabel: "취소",
    action: () => cancelOrder(item)
  })));
  renderProducts();
}

function renderMiniList(container, items) {
  container.replaceChildren();
  if (!items.length) {
    container.append(element("p", "mini-empty", "아직 항목이 없습니다."));
    return;
  }
  items.forEach((item) => {
    const row = element("div", "mini-item");
    const detail = element("strong", "", item.detail);
    row.append(element("span", "", item.name), detail);
    if (item.action) {
      const button = element("button", "mini-action", item.actionLabel);
      button.type = "button";
      button.addEventListener("click", item.action);
      row.append(button);
    }
    container.append(row);
  });
}

async function toggleWish(product, wished) {
  try {
    await api(`/api/wishes/${product.id}`, { method: wished ? "DELETE" : "POST" });
    toast(wished ? "찜 목록에서 삭제했습니다." : "찜 목록에 추가했습니다.");
    await loadAccount();
  } catch (error) { toast(readableError(error)); }
}

async function orderProduct(product) {
  openTransactionModal("order", product);
}

async function cancelOrder(order) {
  openTransactionModal("cancel", order);
}

let activeTransaction = null;

function openTransactionModal(mode, item) {
  const isOrder = mode === "order";
  activeTransaction = { mode, item };
  $("#transactionEyebrow").textContent = isOrder ? "ORDER" : "CANCEL ORDER";
  $("#transactionTitle").textContent = isOrder ? "상품을 주문할까요?" : "주문을 취소할까요?";
  $("#transactionDescription").textContent = isOrder
    ? "수량과 결제 포인트를 확인한 뒤 주문을 완료해 주세요."
    : "취소한 수량만큼 포인트와 상품 재고가 복구됩니다.";
  $("#transactionProduct").textContent = item.productName;
  $("#transactionLimitLabel").textContent = isOrder ? "구매 가능" : "주문 수량";
  $("#transactionLimit").textContent = `${isOrder ? item.stockQuantity : item.quantity}개`;
  $("#transactionTotalLabel").textContent = isOrder ? "결제 포인트" : "환불 포인트";
  $("#transactionConfirm").textContent = isOrder ? "주문하기" : "취소 확정";
  $("#transactionQuantity").max = isOrder ? item.stockQuantity : item.quantity;
  $("#transactionQuantity").value = 1;
  $("#transactionError").textContent = "";
  updateTransactionTotal();
  $("#transactionDialog").showModal();
  $("#transactionQuantity").focus();
  $("#transactionQuantity").select();
}

function closeTransactionModal() {
  $("#transactionDialog").close();
  activeTransaction = null;
}

function updateTransactionTotal() {
  if (!activeTransaction) return;
  $("#transactionError").textContent = "";
  const quantity = Number($("#transactionQuantity").value);
  const total = activeTransaction.item.productPrice * (Number.isFinite(quantity) ? quantity : 0);
  $("#transactionTotal").textContent = `${formatNumber(total)} P`;
}

async function submitTransaction(event) {
  event.preventDefault();
  if (!activeTransaction) return;
  const { mode, item } = activeTransaction;
  const quantity = Number($("#transactionQuantity").value);
  const maximum = mode === "order" ? item.stockQuantity : item.quantity;
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > maximum) {
    $("#transactionError").textContent = `수량은 1부터 ${maximum} 사이의 정수여야 합니다.`;
    return;
  }

  const confirmButton = $("#transactionConfirm");
  confirmButton.disabled = true;
  try {
    await api(mode === "order" ? "/api/customers/order" : "/api/customers/cancel", {
      method: "POST",
      body: JSON.stringify({ productId: item.id || item.productId, quantity })
    });
    closeTransactionModal();
    toast(mode === "order" ? "주문이 완료되었습니다." : "주문을 취소하고 포인트를 돌려드렸습니다.");
    await Promise.all([loadProducts(), loadAccount()]);
  } catch (error) {
    $("#transactionError").textContent = readableError(error);
  } finally {
    confirmButton.disabled = false;
  }
}

async function sendChat(text) {
  const message = text.trim();
  if (!message) return;
  appendMessage("user", message);
  $("#chatInput").value = "";
  const loading = appendMessage("assistant loading", "생각하고 있어요…");
  try {
    const response = await api("/api/ai/chat", {
      method: "POST",
      body: JSON.stringify({ message, conversationId: state.conversationId })
    });
    loading.remove();
    appendMessage("assistant", response.answer);
    await loadPendingActions();
  } catch (error) {
    loading.remove();
    appendMessage("assistant error", readableError(error));
  }
}

function appendMessage(kind, text) {
  const wrapper = element("div", `message ${kind}`);
  wrapper.append(element("span", "message-name", kind.includes("user") ? "YOU" : "AI SHOPPING"), element("p", "", text));
  chatMessages.append(wrapper);
  chatMessages.scrollTop = chatMessages.scrollHeight;
  return wrapper;
}

async function loadPendingActions() {
  const container = $("#pendingActions");
  const actions = await api("/api/ai/actions");
  container.replaceChildren();
  actions.forEach((action) => {
    const card = element("div", "pending-card");
    const button = element("button", "", "확인하고 실행");
    button.type = "button";
    button.addEventListener("click", () => confirmAction(action.confirmationToken));
    card.append(element("p", "", action.confirmationMessage), button);
    container.append(card);
  });
}

async function checkAiAvailability() {
  const status = $("#aiStatus span:last-child");
  try {
    await loadPendingActions();
    status.textContent = "Gemini online";
  } catch (error) {
    status.textContent = error.status === 404 ? "AI 프로필 꺼짐" : "AI 연결 확인 필요";
    $("#reindexButton").disabled = true;
  }
}

async function refreshShop() {
  try {
    await Promise.all([loadProducts(), loadAccount()]);
  } catch (error) { toast(readableError(error)); }
}

async function confirmAction(token) {
  try {
    await api(`/api/ai/actions/${token}/confirm`, { method: "POST" });
    toast("AI 제안 작업을 실행했습니다.");
    await Promise.all([loadProducts(), loadAccount(), loadPendingActions()]);
  } catch (error) { toast(readableError(error)); }
}

async function reindexKnowledge() {
  const button = $("#reindexButton");
  button.disabled = true;
  try {
    const result = await api("/api/ai/knowledge/reindex", { method: "POST" });
    toast(`${result.indexedDocuments}개 문서를 AI 지식에 반영했습니다.`);
  } catch (error) { toast(readableError(error)); }
  finally { button.disabled = false; }
}

function element(tag, className = "", text = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.textContent = text;
  return node;
}

function empty(text) { return element("div", "empty-state", text); }
function initials(name) { return name.split(/\s+/).map((word) => word[0]).join("").slice(0, 2); }
function formatNumber(value) { return Number(value || 0).toLocaleString("ko-KR"); }
function readableError(error) {
  const messages = {
    INVALID_CREDENTIALS: "아이디 또는 비밀번호를 확인해 주세요.",
    DUPLICATE_CUSTOMER_ID: "이미 사용 중인 아이디입니다.",
    INSUFFICIENT_STOCK: "상품 재고가 부족합니다.",
    INSUFFICIENT_FUNDS: "보유 포인트가 부족합니다.",
    AI_QUOTA_EXCEEDED: "오늘의 Gemini 무료 사용량을 모두 사용했습니다. 할당량이 초기화된 후 다시 시도해 주세요."
  };
  return messages[error.code] || messages[error.message] || error.message || "잠시 후 다시 시도해 주세요.";
}

let toastTimer;
function toast(message) {
  const node = $("#toast");
  node.textContent = message;
  node.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => node.classList.remove("show"), 2800);
}

$("#loginTab").addEventListener("click", () => setAuthMode("login"));
$("#registerTab").addEventListener("click", () => setAuthMode("register"));
$("#authForm").addEventListener("submit", handleAuth);
$("#logoutButton").addEventListener("click", logout);
$("#refreshButton").addEventListener("click", refreshShop);
$("#reindexButton").addEventListener("click", reindexKnowledge);
$("#searchForm").addEventListener("submit", (event) => {
  event.preventDefault();
  loadProducts({ query: $("#searchQuery").value.trim(), maxPrice: $("#maxPrice").value, category: $("#categorySelect").value })
    .catch((error) => toast(readableError(error)));
});
$("#chatForm").addEventListener("submit", (event) => { event.preventDefault(); sendChat($("#chatInput").value); });
$("#transactionForm").addEventListener("submit", submitTransaction);
$("#transactionClose").addEventListener("click", closeTransactionModal);
$("#transactionCancel").addEventListener("click", closeTransactionModal);
$("#transactionQuantity").addEventListener("input", updateTransactionTotal);
$("#quantityDecrease").addEventListener("click", () => {
  $("#transactionQuantity").stepDown();
  updateTransactionTotal();
});
$("#quantityIncrease").addEventListener("click", () => {
  $("#transactionQuantity").stepUp();
  updateTransactionTotal();
});
$("#transactionDialog").addEventListener("click", (event) => {
  if (event.target === $("#transactionDialog")) closeTransactionModal();
});
$("#transactionDialog").addEventListener("close", () => { activeTransaction = null; });
let chatComposing = false;
$("#chatInput").addEventListener("compositionstart", () => { chatComposing = true; });
$("#chatInput").addEventListener("compositionend", () => { chatComposing = false; });
$("#chatInput").addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey && !chatComposing && !event.isComposing && event.keyCode !== 229) {
    event.preventDefault();
    $("#chatForm").requestSubmit();
  }
});
document.querySelectorAll(".quick-prompts button").forEach((button) => button.addEventListener("click", () => sendChat(button.textContent)));

if (state.token && state.customerId) enterApp();
