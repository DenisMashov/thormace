# ThorMace

Purpur / Paper 1.21.11 - Thor'un çekici gibi özel mace.

- `/thormace` (alias: `/thor`, `/mjolnir`) -> mace'i verir
- `/thormace <oyuncu>` -> başkasına verir
- `/thormace reload` -> config yeniler

Mace ile bir kişiye vurunca 5 kez yıldırım düşer ve vurulan yerin
etrafındaki (yarıçap 5) bloklar havaya uçup geri iner. Vurulan kişiye 5 sn körlük + ekranda kırmızı THOR'S CURSE yazısı gelir.
Gerçek bloklar
bozulmaz (sadece görsel animasyon). Tüm ayarlar `config.yml` içinde.

## Derleme
GitHub'a yükle -> Actions -> Build -> `ThorMace` artifact (jar).
Yerelde: `mvn package`

## Permission
- thormace.give (op) / thormace.use (herkes) / thormace.admin (op)
