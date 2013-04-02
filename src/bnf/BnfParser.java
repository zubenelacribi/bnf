package bnf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import bnf.BnfDefParser.Tree;

public class BnfParser extends Parser {

	@Override
	public void initialize() {
		try {
			BufferedReader inp = new BufferedReader(new FileReader("bnf.bnf"));
			int lineNo = 0;
			while (true) {
				String line = inp.readLine();
				lineNo++;
				if (line == null) {
					break;
				}
				int colon = line.indexOf(':');
				assert colon >= 0 : "Colon expected in line " + lineNo;
				String def = line.substring(0, colon);
				Tree t = BnfDefParser.parse(line.substring(colon + 1).trim());
				t.parent = new Tree(def, 0, def.length(), null);
				definitions.put(def, t);
			}
			inp.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected Tree extension(Tree t, String s, int begin, int end)
			throws ParseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean keyword(String s) {
		return false;
	}
	
}
