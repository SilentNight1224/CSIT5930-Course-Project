function performSearch(query) {
    if (query == null || query === '') {
        alert("Query cannot be empty.");
        return;
    }
    window.location.href = `/search?q=${encodeURIComponent(query)}`;
}

function search() {
    const query = document.getElementById('search-input').value;
    performSearch(query);
}

function initSearchEngine() {
    const modal = new bootstrap.Modal(document.getElementById("initModal"), {
        keyboard: false
    });
    modal.show();
    fetch('/init')
        .then(response => {
            if (!response.ok) {
                throw new Error(response.statusText);
            }
            return response.json();
        })
        .then(data => {
            if (data.code === 1) {
                modal.hide();
            } else {
                modal.hide();
                alert(data.message);
            }
        })
        .catch(error => {
            modal.hide();
            alert(error.message);
        });
}

function getSummary(url) {
    const modal = new bootstrap.Modal(document.getElementById("aiSummaryModal"), {
        keyboard: false
    });
    const content = document.getElementById("aiModalContent");
    const loading = document.getElementById("aiModalLoading");
    const controller = new AbortController();
    const close = document.getElementById("aiModalClose");
    const open = document.getElementById("aiModalOpen");
    content.classList.add("d-none");
    loading.classList.remove("d-none");
    open.href = url;
    close.addEventListener("click", function() {
        if (!controller.signal.aborted) {
            controller.abort();
        }
        modal.hide();
    })
    modal.show();
    fetch(`/summary?url=${encodeURIComponent(url)}`, {
        signal: controller.signal
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(response.statusText);
            }
            return response.json()
        })
        .then(data => {
            if (data.code === 1) {
                content.innerHTML = data.message;
                content.classList.remove("d-none");
                loading.classList.add("d-none");
            } else {
                throw new Error(data.message)
            }
        })
        .catch(error => {
            modal.hide();
            console.log(error);
            if (error.name !== 'AbortError') {
                alert(error.message);
            }
        })
}

function clearSearchHistory() {
    const modal = new bootstrap.Modal(document.getElementById("clearModal"), {
        keyboard: false
    });
    modal.show();
}

function saveSearchRecord(searchTerm) {
    let searches = JSON.parse(localStorage.getItem('searches')) || [];
    searches.push(searchTerm);
    localStorage.setItem('searches', JSON.stringify(searches));
}

function clearSearchRecord() {
    localStorage.removeItem('searches');
}

function getSearchRecords() {
    return JSON.parse(localStorage.getItem('searches')) || [];
}
