# E-İmza Masaüstü Programı API Entegrasyon Dokümanı

Bu doküman, E-İmza Masaüstü (Launcher) uygulaması geliştiricileri için hazırlanmıştır. Uygulamanın web servisi ile nasıl iletişim kuracağını, kimlik doğrulama süreçlerini ve gerekli parametreleri açıklar.

## 1. Genel Bilgiler

*   **Sunucu Adresi (Base URL):** `https://[DOMAIN]` (Örn: `https://eimza.globalpozitif.com.tr`)
*   **API Endpoint Yapısı:** `[Base URL]/?/webservice/[Service]/[Method]/[Action]`
*   **İstek Tipi (Method):** `POST`
*   **Veri Formatı:** JSON

Tüm isteklerde `Content-Type: application/json` başlığı kullanılması önerilir.

## 2. Kimlik Doğrulama (Authentication)

Masaüstü uygulaması, kullanıcının lisans durumunu doğrulamak ve oturum açmak için aşağıdaki servisleri kullanmalıdır.

### 2.1. E-Posta ve Şifre ile Giriş (Login)

Kullanıcının ilk defa giriş yaptığı veya oturumunun sonlandığı durumlarda kullanılır.

*   **URL:** `/?/webservice/member/post/checkLastOrderByEmailAndPassword`
*   **Method:** `POST`

**İstek Parametreleri (Request Body):**

| Parametre    | Tip    | Zorunlu | Açıklama |
| :---         | :---   | :---    | :---     |
| `email`      | String | Evet    | Kullanıcı e-posta adresi |
| `password`   | String | Evet    | Kullanıcı şifresi (Plain text gönderilmeli, sunucu taraflı şifrelenir) |
| `computerId` | String | Evet    | Cihaza özel benzersiz kimlik (UUID vb.). Lisans kontrolü ve oturum limiti (Max 3 cihaz) için zorunludur. |
| `rememberMe` | String | Hayır   | `'true'` gönderilirse, yanıt içinde bir `remember_token` döner. |

**Başarılı Yanıt (Success Response):**

```json
{
    "status": "success",
    "message": "Giriş Başarılı",
    "expireTime": "2026-05-20 14:30:00", // Lisans bitiş tarihi
    "keyCode": "...", // Şifreleme anahtarı (Gerekirse)
    "user_info": {
        "id": "123",
        "name": "Ad Soyad",
        "email": "ornek@email.com"
    },
    "remember_token": "abc123xyz..." // Sadece rememberMe=true ise döner
}
```

**Hata Yanıtı (Error Response):**

```json
{
    "status": "error",
    "message": "E-posta veya şifre hatalı" // veya "Lisansınız sona ermiştir", "Bu hesap birden çok cihazda kullanılıyor"
}
```

### 2.2. Token ile Otomatik Giriş (Auto Login)

Kullanıcı "Beni Hatırla" seçeneğini işaretlediyse, sonraki açılışlarda şifre sormadan giriş yapmak için kullanılır.

*   **URL:** `/?/webservice/member/post/loginWithToken`
*   **Method:** `POST`

**İstek Parametreleri:**

| Parametre        | Tip    | Zorunlu | Açıklama |
| :---             | :---   | :---    | :---     |
| `email`          | String | Evet    | Kullanıcı e-posta adresi |
| `remember_token` | String | Evet    | Login işleminden dönen token |

**Yanıt:** Başarılı giriş yanıtı Login servisi ile aynıdır. Token geçersizse `status: error` döner ve kullanıcıdan tekrar şifre istenmelidir.

### 2.3. Cihaz Doğrulama (Validate Device)

Uygulama çalışırken veya belirli aralıklarla oturumun hala geçerli olduğunu kontrol etmek için kullanılır.

*   **URL:** `/?/webservice/member/post/validateComputerId`
*   **Method:** `POST`

**İstek Parametreleri:**

| Parametre    | Tip    | Zorunlu | Açıklama |
| :---         | :---   | :---    | :---     |
| `email`      | String | Evet    | Kullanıcı e-posta adresi |
| `computerId` | String | Evet    | Cihaz ID |

### 2.4. Çıkış Yap (Logout)

*   **URL:** `/?/webservice/member/post/logout`
*   **Method:** `POST`

**İstek Parametreleri:**

| Parametre    | Tip    | Zorunlu | Açıklama |
| :---         | :---   | :---    | :---     |
| `email`      | String | Evet    | Kullanıcı e-posta adresi |
| `computerId` | String | Hayır   | Gönderilirse sadece o cihazdan çıkış yapılır. Gönderilmezse tüm cihazlardan (web/mobil/masaüstü) tokenlar silinir. |

## 3. Oturum ve Lisans Kuralları



1.  **Cihaz Limiti:** Bir kullanıcı hesabı en fazla **2 farklı cihazda** (Masaüstü, Mobil, Web toplam) kullanılabilir. 2'den fazla cihazda oturum açılmaya çalışıldığında API hata döner.
2.  **Lisans Süresi:**
    *   **Deneme Sürümü:** İlk kez giriş yapan kullanıcılar için otomatik olarak **7 günlük** deneme başlatılır.
    *   **Satın Alınmış Lisans:** Son sipariş tarihinden itibaren **1 yıl** geçerlidir.
3.  **Computer ID:** Masaüstü uygulaması, kurulduğu bilgisayar için kalıcı ve benzersiz bir `computerId` üretmeli ve bunu saklamalıdır. Bu ID değişirse sistem bunu yeni bir cihaz olarak algılar.

## 4. Dosya İşlemleri (XML Dönüşüm)

E-İmza XML dosyalarını görüntülemek (HTML'e çevirmek) için aşağıdaki servis kullanılır.

*   **URL:** `/?/webservice/eimza/post/process`
*   **Method:** `POST`
*   **Content-Type:** `multipart/form-data`

**Parametreler:**

| Parametre        | Tip  | Zorunlu | Açıklama |
| :---             | :--- | :---    | :---     |
| `xml_file[]`     | File | Evet    | Yüklenecek XML dosyaları (Multiple input destekler) |
| `token`          | String| Hayır  | Üye girişi yapılmışsa token gönderilmelidir. Token yoksa "Ziyaretçi" modunda çalışır ve limit uygulanır. |
| `action`         | String| Evet   | `process` değeri gönderilmelidir. |

Küçük not: `EImza.php` dosyasında `action` form data içinden de okunabiliyor ancak Router yapısı gereği URL'den de `process` olarak gelmesi gerekir. Güvenlik için her ikisini de sağlayabilirsiniz.
