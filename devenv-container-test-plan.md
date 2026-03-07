# PostgreSQL コンテナ起動テスト計画

Docker Compose と Podman kube play で同一の動作をすることを確認する。

## 前提条件の確認

```bash
docker compose version
podman --version
# ポート 5432 が空いていること
```

---

## Phase 1: Docker Compose で起動・検証

```bash
# 1. 起動
docker compose up -d

# 2. ヘルスチェックが healthy になるまで待つ
docker compose ps

# 3. 接続テスト
pg_isready -h localhost -p 5432 -U appuser -d appdb

# 4. 認証・DB操作テスト
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "SELECT version();"
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "CREATE TABLE test_tbl (id serial PRIMARY KEY, val text);"
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "INSERT INTO test_tbl(val) VALUES ('hello');"
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "SELECT * FROM test_tbl;"

# 5. 停止・クリーンアップ
docker compose down -v
```

確認項目:
- [ ] コンテナが `healthy` になる
- [ ] `pg_isready` が `accepting connections` を返す
- [ ] `appuser`/`apppassword` で `appdb` に接続できる
- [ ] DDL/DML が正常に実行できる

---

## Phase 2: Podman kube play で起動・検証

```bash
# 1. Pod の起動
podman kube play pod.yaml

# 2. Pod の状態確認
podman pod ps
podman ps

# 3. 接続テスト
pg_isready -h localhost -p 5432 -U appuser -d appdb

# 4. 認証・DB操作テスト (Phase 1 と同一コマンド)
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "SELECT version();"
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "CREATE TABLE test_tbl (id serial PRIMARY KEY, val text);"
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "INSERT INTO test_tbl(val) VALUES ('hello');"
PGPASSWORD=apppassword psql -h localhost -p 5432 -U appuser -d appdb -c "SELECT * FROM test_tbl;"

# 5. クリーンアップ
podman kube down pod.yaml
podman volume rm postgres-data
```

確認項目:
- [ ] Pod が `Running` になる
- [ ] `pg_isready` が `accepting connections` を返す
- [ ] `appuser`/`apppassword` で `appdb` に接続できる
- [ ] DDL/DML が正常に実行できる

---

## Phase 3: 差異の比較

| 確認項目 | Docker Compose | Podman kube play |
|---|---|---|
| 起動時間 | 計測 | 計測 |
| `SELECT version()` の出力 | 記録 | 記録（同一であること）|
| ポート 5432 へのアクセス | ○ | ○ |
| ボリューム永続化 | 再起動後にデータが残るか | 再起動後にデータが残るか |

---

## 注意点

- 両方を同時に起動するとポート 5432 が競合するため、片方を完全に停止してからもう一方を起動する
- `POSTGRES_PASSWORD` を平文で YAML に書いているため、テスト環境限定で使用すること
- Podman を rootless で動かす場合、`podman system service` のソケットが起動済みであること (`/run/user/1000/podman/podman.sock`)

### Podman rootless ソケットの確認方法

```bash
# ソケットファイルの存在確認
ls -la /run/user/1000/podman/podman.sock

# サービスが実際に応答しているか確認（こちらが確実）
podman --remote info

# systemd で管理している場合
systemctl --user status podman.socket
```

ソケットファイルが存在しても古いプロセスの残骸の可能性があるため、`podman --remote info` で応答が返ることを確認する。

起動していない場合:

```bash
# 手動起動
podman system service --time=0 unix:///run/user/1000/podman/podman.sock &

# systemd で恒久的に有効化する場合
systemctl --user enable --now podman.socket
```
