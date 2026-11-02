import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const require = createRequire(import.meta.url);
const puppeteer = require(join(dirname(fileURLToPath(import.meta.url)), '../target/puppeteer/node_modules/puppeteer-core'));

const port = process.env.APP_PORT;
const adminUser = process.env.ADMIN_USER;
const adminPass = process.env.ADMIN_PASS;
const deactivatePath = process.env.DEACTIVATE_SHOT;
const reactivatePath = process.env.REACTIVATE_SHOT;

const browser = await puppeteer.launch({
  executablePath: '/usr/bin/google-chrome',
  headless: true,
  args: [
    '--no-sandbox',
    '--disable-gpu',
    '--window-size=1400,900',
    '--host-resolver-rules=MAP cdnjs.cloudflare.com 127.0.0.1, MAP cdn.jsdelivr.net 127.0.0.1, MAP cdn.datatables.net 127.0.0.1',
  ],
});

const page = await browser.newPage();
await page.goto(`http://127.0.0.1:${port}/login`, { waitUntil: 'networkidle0' });
await page.type('input[name=username]', adminUser);
await page.type('input[name=password]', adminPass);
await Promise.all([
  page.click('button[type=submit]'),
  page.waitForNavigation({ waitUntil: 'networkidle0' }),
]);
await page.goto(`http://127.0.0.1:${port}/users`, { waitUntil: 'networkidle0' });
await new Promise((r) => setTimeout(r, 3500));
await page.screenshot({ path: deactivatePath, fullPage: true });
await page.screenshot({ path: reactivatePath, fullPage: true });
await browser.close();
