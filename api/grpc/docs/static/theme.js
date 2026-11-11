const mermaid = import('https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs');
const diagrams = document.getElementsByClassName("mermaid");
const originalDataTag = "data-original-mermaid";

function saveOriginalMermaidCode() {
    for (const diagram of diagrams) {
        diagram.setAttribute(originalDataTag, diagram.innerHTML);
    }
}

function resetMermaidDiagrams() {
    for (const diagram of diagrams) {
        diagram.innerHTML = diagram.getAttribute(originalDataTag);
        diagram.removeAttribute("data-processed");
    }
}

function renderMermaidDiagrams() {
    mermaid.then(m => {
        const config = {
            theme: theme.value === 'dark' ? 'dark' : 'default',
            startOnLoad: false,
        };
        m.default.initialize(config);
        m.default.run({ nodes: diagrams });
    })
}

window.addEventListener("load", () => {
    saveOriginalMermaidCode();
    renderMermaidDiagrams();
});

const storageKey = 'theme-preference'

const onClick = () => {
    // flip current value
    theme.value = theme.value === 'light'
        ? 'dark'
        : 'light'

    setPreference()
}

const getColorPreference = () => {
    if (localStorage.getItem(storageKey))
        return localStorage.getItem(storageKey)
    else
        return window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark'
            : 'light'
}

const setPreference = () => {
    localStorage.setItem(storageKey, theme.value)
    reflectPreference()
}

const reflectPreference = () => {
    const body = document.querySelector('body');
    const html = document.querySelector('html'); // The html element is also configured to make sure the background color is correct on mobile if the content overflows horizontally.

    if (theme.value === 'dark') {
        body.className = 'dark';
        html.className = 'dark';
    } else {
        body.className = '';
        html.className = '';
    }

    resetMermaidDiagrams();
    renderMermaidDiagrams();

    document.firstElementChild
        .setAttribute('data-theme', theme.value)

    document
        .querySelector('#theme-toggle')
        ?.setAttribute('aria-label', theme.value)
}

const theme = {
    value: getColorPreference(),
}

window.addEventListener(
    "load",
    () => {
        // set on load so screen readers can see latest value on the button
        reflectPreference()

        // now this script can find and listen for clicks on the control
        document
            .querySelector('#theme-toggle')
            .addEventListener('click', onClick)
    });

// sync with system changes
window
    .matchMedia('(prefers-color-scheme: dark)')
    .addEventListener('change', ({matches:isDark}) => {
        theme.value = isDark ? 'dark' : 'light'
        setPreference()
    });
