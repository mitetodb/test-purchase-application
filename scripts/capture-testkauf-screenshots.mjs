import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import { join } from 'node:path';

const baseUrl = process.env.APP_BASE_URL || 'http://127.0.0.1:8080';
const outDir = process.env.OUT_DIR || '/opt/cursor/artifacts/screenshots/testkauf';

await mkdir(outDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
const desktop = { width: 1440, height: 900 };
const mobile = { width: 390, height: 844 };

async function shot(page, name) {
  const path = join(outDir, name);
  await page.screenshot({ path, fullPage: name.includes('product-full') });
  console.log('saved', path);
}

for (const lang of ['bg', 'en', 'de']) {
  const page = await browser.newPage({ viewport: desktop });
  await page.goto(`${baseUrl}/?lang=${lang}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await shot(page, `product-${lang}-1440.png`);
  const contact = page.locator('#kontakt');
  await contact.scrollIntoViewIfNeeded();
  await page.waitForTimeout(600);
  await shot(page, `contact-${lang}-1440.png`);
  await page.close();
}

for (const lang of ['bg', 'en', 'de']) {
  const page = await browser.newPage({ viewport: desktop });
  await page.goto(`${baseUrl}/login?lang=${lang}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await shot(page, `login-${lang}-1440.png`);
  await page.close();
}

for (const lang of ['bg', 'en', 'de']) {
  const page = await browser.newPage({ viewport: desktop });
  await page.goto(`${baseUrl}/login?error&lang=${lang}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await shot(page, `login-error-${lang}-1440.png`);
  await page.close();
}

for (const lang of ['bg', 'en', 'de']) {
  const page = await browser.newPage({ viewport: mobile });
  await page.goto(`${baseUrl}/?lang=${lang}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await shot(page, `product-${lang}-390.png`);
  await page.close();

  const loginPage = await browser.newPage({ viewport: mobile });
  await loginPage.goto(`${baseUrl}/login?lang=${lang}`, { waitUntil: 'networkidle' });
  await loginPage.waitForTimeout(800);
  await shot(loginPage, `login-${lang}-390.png`);
  await loginPage.close();
}

await browser.close();
