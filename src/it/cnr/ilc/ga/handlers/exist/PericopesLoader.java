package it.cnr.ilc.ga.handlers.exist;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import it.cnr.ilc.ga.model.analysis.Analysis;
import it.cnr.ilc.ga.model.analysis.Form;
import it.cnr.ilc.ga.model.analysis.Lemma;
import it.cnr.ilc.ga.model.analysis.LinguisticUnit;
import it.cnr.ilc.ga.model.analysis.PartOfSpeech;
import it.cnr.ilc.ga.model.analysis.Root;
import it.cnr.ilc.ga.model.comment.PericopeComments;
import it.cnr.ilc.ga.model.indexsearch.PericopeSet;
import it.cnr.ilc.ga.model.pericope.Pericope;
import it.cnr.ilc.ga.model.pericope.Text;
import it.cnr.ilc.ga.model.pericope.Text.LangType;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMFactory;
import org.jdom.input.*;
import org.exist.xmldb.EXistResource;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

public class PericopesLoader {


	final int ID=0;
	final int PERICOPE_AR=1; 
	final int PERICOPE_GR=2; 
	final int ID_AR=3;
	final int ID_GR=4;
	final int INFO_AR=5; 
	final int INFO_GR=6; 
	final int NOTA=7;
	final int ANALYSIS_AR=8;
	final int ANALYSIS_GR=9;
	
	protected final String driver = "org.exist.xmldb.DatabaseImpl";
	private Collection root = null;
//	private PericopeSet pericopeSet;
	
	List<Element> arabicWords = null;
	List<Element> greekWords = null;
	
	Analysis arabicAnalysis = null;
	Analysis greekAnalysis = null;
	
	public boolean connect() {
        try {
            
            Class<?> c = Class.forName(driver);
            Database db = (Database)c.newInstance();
            DatabaseManager.registerDatabase(db);
            root = DatabaseManager.getCollection("xmldb:exist://localhost:8088/xmlrpc/db/femapericopes","admin","angelodel80");
            return true;
            
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (XMLDBException ex) {
            ex.printStackTrace();
        }
        return false;
    }
	
	public PericopesLoader() {
		System.err.println("in pericope Loader FEMA");
	}

	public LinkedHashMap<String, PericopeComments> load(PericopeSet pericopeSet) throws Exception{
		System.err.println(connect());
		//this.pericopeSet = pericopeSet;

		LinkedHashMap<String, PericopeComments> lhm = new LinkedHashMap<String, PericopeComments>(10000);
		int i = 0;
		for(String resource: root.listResources()){
			i++;
			System.out.println("prima, n. " + i);
			XMLResource xr=(XMLResource)root.getResource(resource);
			System.err.println("dopo - " + resource);
			//System.out.println(((EXistResource)xr).g);
			SAXHandler saxhandler=new SAXHandler();
			System.err.println("dopo 1");
			xr.getContentAsSAX(saxhandler);
			Document document=saxhandler.getDocument();
			Element addNode=document.getRootElement();
			Element docNode=addNode.getChild("doc");
			System.err.println("dopo 2");
			
			List<Element> fields=docNode.getChildren();
			
			String pericopeIdStr = fields.get(ID).getTextNormalize();
			if ( -1 == pericopeIdStr.indexOf('.')) {
				pericopeIdStr =  pericopeIdStr + ".0";
			}
			double pericopeId = Double.parseDouble(pericopeIdStr);
			double arabicTextId = Double.parseDouble(fields.get(ID_AR).getTextNormalize());
			double greekTextId = Double.parseDouble(fields.get(ID_GR).getTextNormalize());
			String arabicTextPericope = fields.get(PERICOPE_AR).getTextNormalize();
			String greekTextPericope = fields.get(PERICOPE_GR).getTextNormalize();
			String arabicTextInfo = fields.get(INFO_AR).getTextNormalize();
			String greekTextInfo = fields.get(INFO_GR).getTextNormalize();
			String pericopeNota = fields.get(NOTA).getTextNormalize();
			//System.err.println("dopo 3");
			
			
				
		try{
			arabicWords = fields.get(ANALYSIS_AR).getChildren();
		}catch(Exception ex){
			arabicWords=new ArrayList<Element>();
		}
		try{
			greekWords = fields.get(ANALYSIS_GR).getChildren();
		}catch(Exception ex){
			greekWords=new ArrayList<Element>();
		}
			
			
			
			Pericope pericope=new Pericope();
			pericope.setId(pericopeId);
			Text arabicText = new Text();
			Text greekText = new Text();
			
			arabicAnalysis = new Analysis();
			greekAnalysis = new Analysis();
			
			arabicText.setLangType(LangType.ARABIC);
			//ripulire testo e inserire tag
			
			arabicTextPericope = arabicTextPericope.replaceAll("¶","<br />")
			.replaceAll("‐","-")
			.replaceAll("_", "&nbsp;")
			.replaceAll("\\«","<em class=\"integration\">")
			.replaceAll("\\»","</em>")
			;
			
			arabicText.setContent(arabicTextPericope);
			arabicText.setId(arabicTextId);
			arabicText.setOffset(0);
			arabicText.setReference(arabicTextInfo);
			
			greekText.setLangType(LangType.GREEK);
			
			//ripulire testo e inserire tag
			
			greekTextPericope = greekTextPericope.replaceAll("¶","<br />")
			.replaceAll("‐","-")
			.replaceAll("_", "&nbsp;")
			.replaceAll("\\«","<em class=\"integration\">")
			.replaceAll("\\»","</em>")
			;
			
			greekText.setContent(greekTextPericope);
			greekText.setId(greekTextId);
			greekText.setOffset(0);
			greekText.setReference(greekTextInfo);
			
			
			// inserire funzione per istanziare l'analisi linguistica
			
			boolean lang1OK = parseWords(arabicWords,arabicAnalysis,1);
			boolean lang2OK = parseWords(greekWords,greekAnalysis,2);
			
			System.out.println(lang1OK);
			System.out.println(lang2OK);
			
			String provaAnalisi1 = arabicAnalysis.getAnalysis().get(0).getRoot().getValue();
			String provaAnalisi2 = arabicAnalysis.getAnalysis().get(0).getForm().getNormalizedValue();
			String provaAnalisi3 = greekAnalysis.getAnalysis().get(0).getForm().getNormalizedValue();
			String provaAnalisi4 = greekAnalysis.getAnalysis().get(0).getLemma().getValue();
			System.out.println(provaAnalisi4);
			
			
//			for(Element w : arabicWords){
//				LinguisticUnit arlu = new LinguisticUnit();
//				String prog = w.getAttributeValue("prog");
//				String id = w.getAttributeValue("id");
//				String start = w.getAttributeValue("start");
//				String end = w.getAttributeValue("end");
//				String form = w.getAttributeValue("form");
//				String br = w.getAttributeValue("BR");
//				String lemma = w.getAttributeValue("lemma");
//				String root = w.getAttributeValue("root");
//				String pos = w.getAttributeValue("pos");
//				String voc = w.getAttributeValue("voc");
//				
//				arlu.setLanguage(Text.LangType.ARABIC);
//				arlu.setId(id);
//				arlu.setPositionStart(Integer.parseInt(start, 10));
//				arlu.setPositionEnd(Integer.parseInt(end, 10));
//				
//				Form arluForm = new Form();
//				Lemma arluLemma = new Lemma();
//				Root arluRoot = new Root();
//				PartOfSpeech arluPOS = new PartOfSpeech();
//				
//				arluForm.setValue(form);
//				arluForm.setExtendedValue(voc);
//				arlu.setForm(arluForm);
//				
//				
//				arluLemma.setValue(lemma);
//				arlu.setLemma(arluLemma);
//				
//				arluPOS.setValue(pos);
//				arlu.setPos(arluPOS);
//				
//				root = root.replaceAll("#", "");
//				arluRoot.setValue(root);
//				arlu.setRoot(arluRoot);
//				
//				arabicAnalysis.addLinguisticUnit(arlu);
//				
//			}
			
//			for(Element w : greekWords){
//				LinguisticUnit grlu = new LinguisticUnit();
//				String prog = w.getAttributeValue("prog");
//				String id = w.getAttributeValue("id");
//				String bibref = w.getAttributeValue("bibref");
//				String ucform = w.getAttributeValue("ucform");
//				String start = w.getAttributeValue("start");
//				String end = w.getAttributeValue("end");
//				String form = w.getAttributeValue("form");
//				String lemma = w.getAttributeValue("lemma");
//				String pos = w.getAttributeValue("pos");
//				
//				grlu.setLanguage(Text.LangType.GREEK);
//				grlu.setId(id);
//				grlu.setPositionStart(Integer.parseInt(start, 10));
//				grlu.setPositionEnd(Integer.parseInt(end, 10));
//				
//				Form grluForm = new Form();
//				Lemma grluLemma = new Lemma();
//				PartOfSpeech grluPOS = new PartOfSpeech();
//				
//				grluForm.setValue(form);
//				grluForm.setExtendedValue(ucform);
//				grlu.setForm(grluForm);
//				
//				grluLemma.setValue(lemma);
//				grlu.setLemma(grluLemma);
//				
//				grluPOS.setValue(pos);
//				grlu.setPos(grluPOS);
//				
//				greekAnalysis.addLinguisticUnit(grlu);
//				
//			}
			
			arabicText.setAnalysis(arabicAnalysis);
			greekText.setAnalysis(greekAnalysis);
			
			pericope.setArabicText(arabicText);
			pericope.setGreekText(greekText);
			
			pericopeSet.addPericope(pericope);
			lhm.put(pericopeIdStr, null);
		}
		
		//TODO DISCONNECT Exist
		return lhm ;
		  
	}
	
	private boolean parseWords(List<Element> words, Analysis analysis, int lang){
		for(Element w : words){
			LinguisticUnit lu = new LinguisticUnit();
			String prog = w.getAttributeValue("prog");
			String id = w.getAttributeValue("id");
			String start = w.getAttributeValue("start");
			String end = w.getAttributeValue("end");
			String form = w.getAttributeValue("form");
			String normalizedForm = w.getAttributeValue("normalizedform");
			String lemma = w.getAttributeValue("lemma");
			String pos = w.getAttributeValue("pos");
			String status = w.getAttributeValue("status");
			String root = "";
			if(lang==1){
			root = w.getAttributeValue("root");
			}
			//String voc = w.getAttributeValue("voc");
			
			
			// TODO gestione della lingua per l'unità linguistica
			//lu.setLanguage(Text.LangType.ARABIC); 
			lu.setId(id);
			lu.setPositionStart(Integer.parseInt(start, 10));
			lu.setPositionEnd(Integer.parseInt(end, 10));
			lu.setLanguage(Text.LangType.GREEK);
			
			Form luForm = new Form();
			Lemma luLemma = new Lemma();
			PartOfSpeech luPOS = new PartOfSpeech();
			Root luRoot = new Root();
			
			
			luForm.setValue(form);
			luForm.setExtendedValue(normalizedForm);
			luForm.setNormalizedValue(normalizedForm);
			lu.setForm(luForm);
			
			//gestione di lemmi multipli
			int i = lemma.indexOf(" ");
			lemma= (i==-1)? lemma:lemma.substring(0, i);
			luLemma.setValue(lemma);
			lu.setLemma(luLemma);
		
		if (lang==1){
			lu.setLanguage(Text.LangType.ARABIC);
			try{
				root = root.replaceAll("#", "NaN");
				luRoot.setValue(root);
	 			lu.setRoot(luRoot);
			}catch(Exception ex){
					ex.printStackTrace();
			}
		}
			
			luPOS.setValue(pos);
			lu.setPos(luPOS);
			
			analysis.addLinguisticUnit(lu);
			
		}

		return true;
	}
	
	public static void main(String[] args){
		try{
			PericopesLoader pl=new PericopesLoader();
			//System.err.println(pl.connect());
			PericopeSet pset = new PericopeSet();
			
			LinkedHashMap<String, PericopeComments> ris=pl.load(pset);
			System.out.println(ris);
//			for (Pericope p : pset.getPericopeSet()) {
//				try {
//					System.out.println(p.getArabicText().getAnalysis().getAnalysis().get(0).getLemma().getValue());
//				}catch(Exception ex){
//					//
//				}
//			}
		}catch(Exception ex){
			ex.printStackTrace(System.err);
		}
	}
}
