document.addEventListener("DOMContentLoaded", function () {
        const boxes = document.querySelectorAll(".my-reservation-box");

        boxes.forEach((box) => {
          box.addEventListener("click", function () {
            const details = box.querySelector(".my-reservation-details");

            if (!details) return;

            // 이미 열려 있으면 닫기
            if (details.style.display === "block") {
              details.style.display = "none";
            } else {
              // 다른 박스들은 닫기 (선택)
              document
                .querySelectorAll(".my-reservation-details")
                .forEach((d) => {
                  d.style.display = "none";
                });
              // 현재 박스 열기
              details.style.display = "block";
            }
          });
        });

        const stars = document.querySelectorAll(".star-rating .star");
        const ratingInput = document.getElementById("ratingInput");

        stars.forEach((star, index) => {
          star.addEventListener("click", () => {
            const rating = star.getAttribute("data-value");
            ratingInput.value = rating;

            // 별 색상 업데이트
            stars.forEach((s, i) => {
              s.classList.toggle("selected", i < rating);
            });
          });
        });

        //이미지 미리보기

        const imageInput = document.getElementById("reviewImages");
        const previewContainer = document.getElementById("previewContainer");
        let uploadedFiles = [];

        imageInput.addEventListener("change", function () {
          const files = Array.from(this.files);

          files.forEach((file) => {
            if (!file.type.startsWith("image/")) return;
            if (uploadedFiles.length >= 10) return;
            uploadedFiles.push(file);
          });

          renderPreviews();
          syncInputFiles();
        });

        function renderPreviews() {
          previewContainer.innerHTML = "";

          uploadedFiles.forEach((file, index) => {
            const reader = new FileReader();

            reader.onload = function (e) {
              const previewDiv = document.createElement("div");
              previewDiv.className = "image-preview";

              const img = document.createElement("img");
              img.src = e.target.result;

              const removeBtn = document.createElement("button");
              removeBtn.className = "remove-image";
              removeBtn.textContent = "×";

              removeBtn.addEventListener("click", () => {
                uploadedFiles.splice(index, 1);
                renderPreviews();
                syncInputFiles();
              });

              previewDiv.appendChild(img);
              previewDiv.appendChild(removeBtn);
              previewContainer.appendChild(previewDiv);
            };

            reader.readAsDataURL(file);
          });
        }

        function syncInputFiles() {
          const dataTransfer = new DataTransfer();
          uploadedFiles.forEach((file) => dataTransfer.items.add(file));
          imageInput.files = dataTransfer.files;
        }
      });

      // 모달열고 닫기

      function openCreateReviewModal(reservationId) {
        document.getElementById("reservationIdInput").value = reservationId;
        document.getElementById("createReviewModal").style.display = "block";
      }

      function closeCreateReviewModal() {
        document.getElementById("createReviewModal").style.display = "none";
      }