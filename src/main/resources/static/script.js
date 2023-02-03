const form = document.getElementById("form"), btn = document.getElementById("file");

function copyTable() {
    let copy = document.getElementById("copyLabel");
    copy.innerText = 'Скопированно!';
    setTimeout(() => copy.innerText = 'Копировать таблицу', 1250);
    let textArea = document.getElementById("copy-area");
    textArea.innerText = document.getElementById("table").value;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    return new Promise((res, rej) => {
        document.execCommand('copy') ? res() : rej();
    });
}

btn.disabled = false;
btn.addEventListener('change', function () {
    let file = this.files[0];
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
});