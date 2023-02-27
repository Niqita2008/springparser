const btn = document.getElementById("file"), form = document.getElementById("form");

const copyTable = () => {
    const copy = document.querySelector('#copyLabel')
    copy.innerText = 'Скопированно!';
    setTimeout(() => copy.innerText = 'Копировать таблицу', 1250);
    const textArea = document.createElement("textarea");
    textArea.value = document.getElementById("table").innerText;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
        document.execCommand('copy');
    } catch (err) {
        console.error('Unable to copy to clipboard', err);
    }
    document.body.removeChild(textArea);
}

btn.disabled = false;
btn.addEventListener('change', (event) => {
    const file = event.target.files[0];
    if (!file.name.endsWith('.pdf')) {
        alert('Выбрете файл *.pdf');
        return;
    }
    if (file.size > 8_388_608 || file.size < 65_536) { // from 64kb to 8mb
        alert('Файл не подходит по размерам');
        return;
    }
    form.submit();
    btn.disabled = true;
}, false);