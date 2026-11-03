(function () {
  function findFileInput(form) {
    var fileInput = form.querySelector('input[type="file"][name="file"]');
    if (fileInput) {
      return fileInput;
    }
    return form.querySelector('input[type="file"][name="imageFile"]');
  }

  function findClientErrorElement(form) {
    if (form.id === 'attachment-upload-form') {
      return document.getElementById('attachment-upload-client-error');
    }
    if (form.id === 'profile-upload-form') {
      return document.getElementById('profile-upload-client-error');
    }
    return null;
  }

  function bindUploadForm(form) {
    var maxBytes = parseInt(form.getAttribute('data-max-upload-bytes') || '', 10);
    var tooLargeMessage = form.getAttribute('data-upload-too-large-msg') || '';
    if (!Number.isFinite(maxBytes) || maxBytes <= 0) {
      return;
    }

    form.addEventListener('submit', function (event) {
      var fileInput = findFileInput(form);
      var errorEl = findClientErrorElement(form);
      if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
        if (errorEl) {
          errorEl.classList.add('d-none');
          errorEl.textContent = '';
        }
        return;
      }

      if (fileInput.files[0].size > maxBytes) {
        event.preventDefault();
        if (errorEl && tooLargeMessage) {
          errorEl.textContent = tooLargeMessage;
          errorEl.classList.remove('d-none');
        }
      } else if (errorEl) {
        errorEl.classList.add('d-none');
        errorEl.textContent = '';
      }
    });

    var fileInput = findFileInput(form);
    if (fileInput) {
      fileInput.addEventListener('change', function () {
        var errorEl = findClientErrorElement(form);
        if (errorEl) {
          errorEl.classList.add('d-none');
          errorEl.textContent = '';
        }
      });
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form.js-monitored-upload-form').forEach(bindUploadForm);
  });
})();
