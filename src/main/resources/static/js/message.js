document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("message-form");
  const input = document.getElementById("message-input");
  const receiverId = document.getElementById("receiver-id").value;
  const messageContainer = document.getElementById("message-container");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const messageText = input.value.trim();
    if (messageText === "") return;

    try {
      const response = await fetch("/user/message/send", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          receiverId: receiverId,
          messageText: messageText,
        }),
      });

      if (response.ok) {
        const data = await response.json();

        const messageDiv = document.createElement("div");
        messageDiv.className = "text-right text-sm";

        const bubble = document.createElement("div");
        bubble.className =
          "inline-block px-3 py-2 rounded bg-teal-200 text-gray-700 dark:bg-blue-800 dark:text-blue-100 max-w-xs";
        bubble.innerText = data.messageText;

        messageDiv.appendChild(bubble);
        messageContainer.appendChild(messageDiv);

        messageContainer.scrollTop = messageContainer.scrollHeight;
        input.value = "";
      } else {
        console.error("Failed to send message");
      }
    } catch (error) {
      console.error("Error:", error);
    }
  });
});
