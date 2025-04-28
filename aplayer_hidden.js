var style = document.createElement('style');
style.type = 'text/css';
style.innerHTML = '.aplayer.aplayer-fixed.aplayer-narrow .aplayer-body {left: -66px !important;}' +
                 '.aplayer.aplayer-fixed.aplayer-narrow .aplayer-body:hover {left: 0 !important;}';
document.getElementsByTagName('head')[0].appendChild(style);