# GitHub — repo nhóm

Người dùng đã xác nhận repo **public** `kiethk/car-rental-management-system`.

## Đăng nhập và tạo/push lần đầu

Mở terminal ở thư mục project:

```bash
gh auth login --hostname github.com --git-protocol https --web
gh auth setup-git --hostname github.com
mvn verify
git init -b main
git add .
git status --short
git diff --cached --stat
git commit -m "chore: initialize CRMS Spring MVC team base"
gh repo create kiethk/car-rental-management-system --public --source=. --remote=origin --push
```

Đây là hướng dẫn lần đầu. Nếu Git/commit/repo đã được tạo, bỏ qua bước tương ứng. Nếu repo đã có lịch sử, clone và tạo PR; không force push hoặc ghi đè lịch sử.

CLI docs: [auth login](https://cli.github.com/manual/gh_auth_login), [repo create](https://cli.github.com/manual/gh_repo_create).

## Thành viên bắt đầu

```bash
git clone https://github.com/kiethk/car-rental-management-system.git
cd car-rental-management-system
git switch -c feature/phu-auth
```

Phúc: `feature/phuc-fleet`; Trí: `feature/tri-promotion`; Kiệt: `feature/kiet-slot`.

Sau mỗi thay đổi nhỏ:

```bash
mvn verify
git add .
git commit -m "feat: describe completed behavior"
git push -u origin HEAD
```

Tạo Pull Request vào `main`, ghi rõ chức năng và cách kiểm tra; có một người review trước khi Kiệt merge. Cập nhật nhánh từ main trước khi tiếp tục. CI chạy `mvn verify` khi push main/tạo PR.

## Kiệt bàn giao quyền

- GitHub → repository → Settings → Collaborators → Add people; thêm đúng username Phú, Phúc, Trí để họ push nhánh. Public chỉ cho phép đọc, không tự cấp quyền ghi.
- Có thể bật rule main yêu cầu PR, một reviewer và build check sau khi CI xuất hiện.
- Chưa gửi lời mời tự động vì chưa có username của ba thành viên.
- Không commit `target/`, IDE, `.env`, file local chứa mật khẩu hoặc uploads.
- Mọi thay đổi entity/schema thống nhất qua PR; database đang dùng thì thêm script ALTER có thứ tự.
