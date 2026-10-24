import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {extractPdf} from '../src/pdf.js';
test('real PDF extraction preserves page numbers and content',async()=>{const data=await readFile(new URL('./fixtures/report.pdf',import.meta.url));const pages=await extractPdf(data);assert.equal(pages.length,2);assert.equal(pages[1].number,2);assert.match(pages[0].text,/Nextfleet/);assert.match(pages[1].text,/batterie/);});
test('non PDF payload is rejected',async()=>{await assert.rejects(()=>extractPdf(Buffer.from('<html>not a pdf</html>')),e=>e.statusCode===415);});
