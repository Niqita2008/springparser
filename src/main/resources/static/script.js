const form = document.querySelector('#form'), btn = document.querySelector('#file');

function copyTable() {
    let copy = document.querySelector('#copyLabel')
    copy.innerText = 'Скопированно!';
    setTimeout(() => copy.innerText = 'Копировать таблицу', 1250);
    let textArea = document.querySelector('#copy-label');
    textArea.value = document.querySelector('#table').innerText;
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