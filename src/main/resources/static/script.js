const form = document.getElementById("form"), btn = document.getElementById("file");

const copyTable = () => {
    const copy = document.querySelector('#copyLabel')
    copy.innerText = 'Скопированно!';
    setTimeout(() => copy.innerText = 'Копировать таблицу', 1250);
    const textArea = document.createElement("textarea");
    textArea.value = document.getElementById("table").innerHTML;
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
btn.addEventListener('change', () => {
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