import { ServiceError } from './groq.js';
export async function extractPdf(buffer) {
  if(buffer.subarray(0,5).toString()!=='%PDF-') throw new ServiceError(415,'Ce fichier n’est pas un PDF valide.','INVALID_PDF');
  const {getDocument}=await import('pdfjs-dist/legacy/build/pdf.mjs');
  let document;
  try {
    document=await getDocument({data:new Uint8Array(buffer),isEvalSupported:false,useSystemFonts:true,disableFontFace:true,stopAtErrors:true}).promise;
    if(document.numPages>80) throw new ServiceError(413,'Le rapport doit contenir au maximum 80 pages.','TOO_MANY_PAGES');
    const pages=[];let total=0;
    for(let number=1;number<=document.numPages;number++) {
      const page=await document.getPage(number); const content=await page.getTextContent();
      const text=content.items.map(item=>item.str??'').join(' ').replace(/\s+/g,' ').trim();
      total+=text.length;if(total>100000)throw new ServiceError(413,'Le texte dépasse 100 000 caractères. Importez une partie du rapport.','DOCUMENT_TOO_LONG');
      pages.push({number,text});
    }
    if(total<80) throw new ServiceError(422,'Ce PDF semble scanné. Exportez un PDF avec du texte sélectionnable ou collez le texte.','OCR_REQUIRED');
    return pages;
  } catch(e) {if(e instanceof ServiceError)throw e;throw new ServiceError(422,'Impossible de lire ce PDF. Vérifiez qu’il n’est pas protégé par un mot de passe.','PDF_UNREADABLE');}
  finally {await document?.destroy();}
}
