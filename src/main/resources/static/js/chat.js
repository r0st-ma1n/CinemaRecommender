// Чат без перезагрузки страницы: сообщение отправляется на /api/chat (POST, JSON),
// ответ бота добавляется в ленту. История хранится только на странице.
// Весь текст вставляется через textContent — как текст, а не как HTML,
// поэтому теги в сообщении пользователя (например, <script>) просто отобразятся буквами.

const form = document.getElementById('chatForm');
const input = document.getElementById('chatInput');
const sendButton = document.getElementById('chatSend');
const messages = document.getElementById('chatMessages');
const apiUrl = form.dataset.apiUrl;

form.addEventListener('submit', async (event) => {
    event.preventDefault(); // не перезагружать страницу
    const text = input.value.trim();
    if (!text) {
        return;
    }

    addMessage('user', text);
    input.value = '';
    sendButton.disabled = true;
    const typing = addMessage('bot', 'Бот думает…');

    try {
        const response = await fetch(apiUrl, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({message: text})
        });
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        const reply = await response.json();
        typing.remove();
        const botMessage = addMessage('bot', reply.text);
        for (const movie of reply.movies) {
            botMessage.appendChild(movieCard(movie));
        }
    } catch (error) {
        typing.remove();
        addMessage('bot', 'Не удалось получить ответ. Проверьте, что приложение запущено, и попробуйте ещё раз.');
    } finally {
        sendButton.disabled = false;
        input.focus();
    }
});

/** Добавить сообщение в ленту: who = 'user' (справа) или 'bot' (слева). */
function addMessage(who, text) {
    const message = document.createElement('div');
    message.className = 'chat-message ' + who;

    const bubble = document.createElement('div');
    bubble.className = 'chat-bubble';
    bubble.textContent = text;

    message.appendChild(bubble);
    messages.appendChild(message);
    messages.scrollTop = messages.scrollHeight; // прокрутить к последнему сообщению
    return message;
}

/** Карточка фильма со ссылкой на его страницу. */
function movieCard(movie) {
    const card = document.createElement('a');
    card.className = 'chat-movie card text-decoration-none text-body';
    card.href = MOVIE_URL + movie.id;

    const poster = document.createElement('img');
    poster.src = movie.posterUrl ? movie.posterUrl : NO_POSTER_URL;
    poster.alt = '';
    poster.className = 'poster rounded-start';

    const body = document.createElement('div');
    body.className = 'p-2';

    const title = document.createElement('div');
    title.className = 'fw-semibold';
    title.textContent = movie.title;

    const details = document.createElement('div');
    details.className = 'small text-muted';
    details.textContent = movie.year + ' · ' + movie.type + ' · ★ ' + movie.rating.toFixed(1);

    body.append(title, details);
    card.append(poster, body);
    return card;
}
