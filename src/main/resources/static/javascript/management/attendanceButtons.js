document.addEventListener('DOMContentLoaded', function () {
  const toggleButton = document.getElementById('btn-toggle-attendance');
  const leaveButton = document.getElementById('btn-request-leave');
  const timeDisplay = document.getElementById('attendance-time-display');

  if (!toggleButton || !leaveButton) return;

  const tokenMeta = document.querySelector('meta[name="_csrf"]');
  const headerMeta = document.querySelector('meta[name="_csrf_header"]');
  const token = tokenMeta ? tokenMeta.getAttribute('content') : '';
  const header = headerMeta ? headerMeta.getAttribute('content') : '';

  let isWorking = false;

  // 출근 상태 확인 API 호출
  fetch('/manage/attendance/status', {
    headers: {
      'Content-Type': 'application/json',
      [header]: token
    }
  })
    .then(res => res.json())
    .then(data => {
      const { isWorking: working, clockIn, clockOut } = data;

      isWorking = working;

      if (isWorking) {
        toggleButton.textContent = '퇴근하기';
        toggleButton.style.backgroundColor = '#566a8e';
        timeDisplay.innerHTML = `출근시간: ${clockIn || '-'}`;
      } else if (clockOut) {
        toggleButton.textContent = '오늘 퇴근 완료';
        toggleButton.disabled = true;
        toggleButton.style.backgroundColor = '#aaa';
        timeDisplay.innerHTML = `출근시간: ${clockIn || '-'}<br>퇴근시간: ${clockOut}`;
      } else if (clockIn) {
        // 출근 기록은 있는데 퇴근 기록이 없는 경우 (혹시 있으면 대비)
        toggleButton.textContent = '퇴근하기';
        toggleButton.style.backgroundColor = '#566a8e';
        timeDisplay.innerHTML = `출근시간: ${clockIn}`;
        isWorking = true;
      } else {
        toggleButton.textContent = '출근하기';
        toggleButton.style.backgroundColor = '#8c9ed9';
        timeDisplay.innerHTML = '';
        isWorking = false;
      }

      toggleButton.addEventListener('click', function () {
        const now = new Date();
        const isoTime = now.toISOString();
        const formatted = now.toLocaleString('ko-KR', {
          year: 'numeric', month: '2-digit', day: '2-digit',
          hour: '2-digit', minute: '2-digit'
        });

        if (!isWorking) {
          if (!confirm('출근하시겠습니까?')) return;

          fetch('/manage/attendance/START', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              [header]: token
            },
            body: JSON.stringify(isoTime)
          })
            .then(res => {
              if (!res.ok) throw new Error();
              isWorking = true;
              toggleButton.textContent = '퇴근하기';
              toggleButton.style.backgroundColor = '#566a8e';
              timeDisplay.innerHTML = `출근시간: ${formatted}`;
            })
            .catch(() => alert('출근 등록 실패'));
        } else {
          if (!confirm('퇴근하시겠습니까?')) return;

          fetch('/manage/attendance/END', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              [header]: token
            },
            body: JSON.stringify(isoTime)
          })
            .then(res => {
              if (!res.ok) throw new Error();
              isWorking = false;
              toggleButton.textContent = '오늘 퇴근 완료';
              toggleButton.style.backgroundColor = '#aaa';
              toggleButton.disabled = true;
              timeDisplay.innerHTML += `<br>퇴근시간: ${formatted}`;
            })
            .catch(() => alert('퇴근 등록 실패'));
        }
      });
    })
    .catch(() => {
      alert('출퇴근 상태 정보를 가져오지 못했습니다.');
    });

  leaveButton.addEventListener('click', function () {
    alert('📅 휴가 신청은 추후 구현될 예정입니다!');
  });
});
