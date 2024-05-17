function decodeHtmlEntity(encodedString) {
  var textArea = document.createElement('textarea');
  textArea.innerHTML = encodedString;
  return textArea.value;
}

function renderObfuscatedEmails() {
  let obfuscatedEmailElements = document.querySelectorAll('.obfuscated-email');
  obfuscatedEmailElements.forEach(element => {
    let obfuscatedEmail = element.getAttribute('data-email');
    let email = decodeHtmlEntity(obfuscatedEmail);
    let link = document.createElement('a');
    link.href = 'mailto:' + email;
    link.innerHTML = element.innerHTML;
    element.innerHTML = '';
    element.appendChild(link);
  });
}

renderObfuscatedEmails();
