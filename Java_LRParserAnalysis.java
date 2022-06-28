import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

public class Java_LRParserAnalysis
{
    private static StringBuffer prog = new StringBuffer();

    /**
     *  this method is to read the standard input
     */
    private static void read_prog()
    {
        Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine())
        {
            prog.append(sc.nextLine().trim()+'\n'); //这里自己加了换行符并对输入进行了空格处理
        }
        prog.append(" $");
    }


    // add your method here!!
    /************************************************************* */
    private static List<Production> productions = new ArrayList<>();
    private static List<String> terminals = new ArrayList<>(); //终止符的集合
    private static List<String> nonTerminals = new ArrayList<>(); //非终止符的集合
    private static Map<String, Set<String>> FirstSets = new HashMap<>(); //所有的first集

    private static List<List<Item>> C = new ArrayList<>(); //集族

    private static String[][] ACTION;
    private static Integer[][] GOTO;

    private static Stack<Production> outputStack = new Stack<>();

    private static String progS;
    private static boolean flag=false;

    private static void initGrammar(){ //将文法放到数组里
        String[] grammars = new String[28];
        grammars[0] = "program -> compoundstmt  ";
        grammars[1] = "stmt ->  ifstmt ";
        grammars[2] = "stmt ->  whilestmt ";
        grammars[3] = "stmt ->  assgstmt ";
        grammars[4] = "stmt ->  compoundstmt ";
        grammars[5] = "compoundstmt ->  { stmts }  ";
        grammars[6] = "stmts ->  stmt stmts  ";
        grammars[7] = "stmts ->  E ";
        grammars[8] = "ifstmt ->  if ( boolexpr ) then stmt else stmt  ";
        grammars[9] = "whilestmt ->  while ( boolexpr ) stmt  ";
        grammars[10] = "assgstmt ->  ID = arithexpr ; ";
        grammars[11] = "boolexpr  ->  arithexpr boolop arithexpr  ";
        grammars[12] = "boolop ->   <  ";
        grammars[13] = "boolop ->   >  ";
        grammars[14] = "boolop ->   <= ";
        grammars[15] = "boolop ->   >= ";
        grammars[16] = "boolop ->   == ";
        grammars[17] = "arithexpr  ->  multexpr arithexprprime  ";
        grammars[18] = "arithexprprime ->  + multexpr arithexprprime ";
        grammars[19] = "arithexprprime ->  - multexpr arithexprprime ";
        grammars[20] = "arithexprprime ->  E ";
        grammars[21] = "multexpr ->  simpleexpr  multexprprime  ";
        grammars[22] = "multexprprime ->  * simpleexpr multexprprime ";
        grammars[23] = "multexprprime ->  / simpleexpr multexprprime ";
        grammars[24] = "multexprprime -> E ";
        grammars[25] = "simpleexpr ->  ID  ";
        grammars[26] = "simpleexpr ->  NUM  ";
        grammars[27] = "simpleexpr ->  ( arithexpr ) ";
        constructGrammar(grammars);
    }
    private static void constructGrammar(String[] grammars){        
        for(String grammar : grammars){
            String father = grammar.split("->")[0].trim(); //获取产生式的左边
            String[] sons =  grammar.split("->")[1].trim().split("\\s+");
            for(int i=0;i<sons.length;++i){
                sons[i] = sons[i].trim();
            }
            Production production = new Production(father, sons);
            productions.add(production);
            nonTerminals.add(father); //顺便构造非终结符
        }
    }

    private static void initTerminal(){
        String reservedWord = "{ }  if ( ) then else  while ( )  ID =   > < >= <= ==  + -  * /  ID NUM  E  $  ; ";
        terminals = Arrays.asList(reservedWord.split("\\s+"));
    }

    private static boolean isTerminal(String word){
        return terminals.contains(word);
    }
    
    private static boolean isNonTerminal(String word){
        return nonTerminals.contains(word);
    }




    private static void initFirst(){ //初始化first集
        for(String terminal : terminals){ //每个终止符的first集为自己，首先初始化方便之后的构造
            FirstSets.put(terminal, new HashSet<String>(){{add(terminal);}});
        }
        for(String nonTerminal : nonTerminals){
            constructFirst(nonTerminal); //具体的构造first集的算法
        }
    }
    private static void constructFirst(String X){
        if(FirstSets.get(X)!=null)return; //采用递归剪枝，以免不必要的递归操作
        Set<String> fatherFirst = FirstSets.containsKey(X) ? FirstSets.get(X) : new HashSet<>();
        for(Production production : productions){
            if(production.father.equals(X)){
                String[] words = production.sons;
                for(String word : words){
                    constructFirst(word); //递归
                    Set<String> wordFirst = FirstSets.get(word);
                    fatherFirst.addAll(wordFirst);
                    if(!wordFirst.contains("E")){
                        break;
                    }
                }
                FirstSets.put(X, fatherFirst);
            }
        }
    }


    private static List<Item> clousure(List<Item> I ){
        int lastLen = I.size();
        while(true){
            // for(Item item : setOfItems){
            for(int i=0;i<I.size();++i){
                Item item = I.get(i); //I中的每一个项
                String B = item.curStr; //接下来要分析的，即·后面的字
                for(Production production : productions){
                    if(production.father.equals(B)){ //G'的每一个产生式B->r
                        List<String> betaA = new ArrayList<>();
                        for(int j=item.curPointer+1; j<item.production.sons.length; ++j){
                            betaA.add(item.production.sons[j]);
                        }
                        betaA.add(item.lookAhead);
                        Set<String> first = new HashSet<>(); //计算beta,a的first
                        for(String word : betaA){
                            Set<String> temp = FirstSets.get(word);
                            first.addAll(temp);
                            if(!temp.contains("E")){
                                break;
                            }
                        }
                        for(String b : first){
                            if(isTerminal(b)){ //first(Ba)中的每个终结符号b
                                Item newItem = new Item(production,b,0,production.sons[0]);
                                if(!I.contains(newItem)){
                                    I.add(newItem);
                                }
                            }
                        }
                    }
                }
            }
            int newLen = I.size();
            if(newLen==lastLen) break;
            else lastLen = newLen;
        }
        return I;
    }


    private static List<Item> goTo(List<Item> I, String X){
        List<Item> J = new ArrayList<>();
        for(Item item : I){ //I中的每个项
            if(item.curStr.equals(X)){
                int nextP = item.curPointer +1;
                String nextStr = item.production.sons.length > nextP ? item.production.sons[nextP] : "";
                Item newItem = new Item(item.production, item.lookAhead, nextP, nextStr);
                J.add(newItem);
            }
        }
        return clousure(J);
    }

    private static void items(){
        String[] tempSons = new String[1];
        tempSons[0] = "program";
        Production origin = new Production("program'", tempSons); //扩展文法
        Item expand = new Item(origin, "$", 0, "program"); 
        List<Item> initSet = new ArrayList<>();
        initSet.add(expand);
        C.add(clousure(initSet));

        Set<String> allSymbol = new HashSet<>();
        allSymbol.addAll(nonTerminals);
        allSymbol.addAll(terminals);

        int lastLen = C.size();
        while(true){    //repeat
            for(int i=0; i<C.size(); ++i){
                List<Item> I_i = C.get(i); //C中的每个项集I
                for(String X : allSymbol){ //每个文法符号X
                    List<Item> I_j = goTo(I_i, X);
                    if(!I_j.isEmpty() && !C.contains(I_j)){
                        C.add(I_j);
                    }
                }
            }
            int newLen = C.size();
            if(newLen==lastLen)break;
            else lastLen = newLen;
        }
    }

    private static void constructTable(){
        int statusNum = C.size();
        int terminalNum = terminals.size();
        int nonTerminalNum = nonTerminals.size();
        ACTION = new String[statusNum][terminalNum];
        GOTO = new Integer[statusNum][nonTerminalNum];

        for(int i=0; i<C.size(); ++i){
            List<Item> I_i = C.get(i);
            for(Item item : I_i){
                String a = item.curStr;
                String A = item.production.father;
                if(isTerminal(a)){
                    List<Item> I_j = goTo(I_i, a);
                    int j = C.indexOf(I_j);
                    ACTION[i][terminals.indexOf(a)] = "s_" + j;
                }
                if(a.equals("") && !A.equals("program'")){
                    ACTION[i][terminals.indexOf(item.lookAhead)] = "r_" + productions.indexOf(item.production);
                }
                if(item.production.father.equals("program'") && a.equals("")){
                    ACTION[i][terminals.indexOf("$")] = "acc";
                }

                if(isNonTerminal(a)){
                    List<Item> I_j = goTo(I_i, a);
                    int j = C.indexOf(I_j);
                    GOTO[i][nonTerminals.indexOf(a)] = j;
                }
            }
        }

    }


    private static void parse(){
        Stack<String> symbleStack = new Stack<>();
        Stack<Integer> stateStack = new Stack<>();
        stateStack.push(0);
        progS = prog.toString();
        String[] progStr = progS.trim().split("\\s+");

        int p=0;
        while(true){
            int s = stateStack.peek();
            String a = progStr[p];
            String action = ACTION[s][terminals.indexOf(a)];
            if(action==null) {
                a = "E";
                action=ACTION[s][terminals.indexOf("E")];
                --p;
            }
            if(action.startsWith("s")){
                symbleStack.push(a);
                ++p;
                stateStack.push(Integer.valueOf(action.substring(2)));
            }else if(action.startsWith("r")){
                int productionNo = Integer.valueOf(action.substring(2));
                Production production = productions.get(productionNo);
                int betaM = production.sons.length;
                while(betaM>0){
                    symbleStack.pop();
                    stateStack.pop();
                    --betaM;
                }

                symbleStack.push(production.father);
                int t = stateStack.peek();
                stateStack.push(GOTO[t][nonTerminals.indexOf(production.father)]);
                outputStack.push(production);
            }else if(action.equals("acc")){
                break;
            }
        }
    }



    private static void printResult(){
        List<String> output = new ArrayList<>();
        output.add("program");
        System.out.println("program => ");
        while(!outputStack.isEmpty()){
            Production production = outputStack.pop();
            String father = production.father;
            String[] sons = production.sons;
            for(int i=output.size()-1; i>=0; --i){  //注意是从右往左解析
                if(output.get(i).equals(father)){
                    output.remove(i);
                    for(int j=sons.length-1; j>=0; --j){
                        if(sons[j].equals("E")) continue;
                        output.add(i,sons[j]);
                    }
                    break;
                }
            }
            for(String item : output){
                System.out.print(item +" ");
            }
            if(!outputStack.isEmpty()) System.out.println("=> ");
        }
    }









    private static void init(){
        initGrammar();
        initTerminal();
        initFirst();
        items();
        constructTable();
    }


    /**
     *  you should add some code in this method to achieve this lab
     */
    private static void analysis()
    {
        read_prog();
        init();
        parse();
        printResult();
    }

    /**
     * this is the main method
     * @param args
     */
    public static void main(String[] args) {
        analysis();
    }
}











class Production{
    String father;
    String[] sons;

    public Production(String father, String[] sons){
        this.father = father;
        this.sons = sons;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production production = (Production)o;
        return father.equals(production.father) && sons.equals(production.sons);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(this.father);
        hash = 31 * hash + Arrays.deepHashCode(this.sons);
        return hash;
    }

    @Override
    public String toString(){
        String tempSons = "";
        for(int i=0; i<sons.length; ++i){
            tempSons = tempSons + sons[i] + " ";
        }
        return father + " --> " + tempSons;
    }
}

class Item{
    Production production;
    String lookAhead;
    String curStr;
    int curPointer;
    public Item(Production production,String lh,int curP,String cur){
        this.production = production;
        this.lookAhead = lh;
        this.curPointer = curP;
        this.curStr = cur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item lr1Item = (Item) o;
        return production == lr1Item.production &&
                lookAhead.equals(lr1Item.lookAhead) &&
                curStr.equals(lr1Item.curStr) &&
                curPointer == lr1Item.curPointer;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + this.curPointer;
        hash = 31 * hash + Objects.hashCode(this.lookAhead);
        hash = 31 * hash + Objects.hashCode(this.curStr);
        hash = 31 * hash + Objects.hashCode(this.production);
        return hash;
    }

    @Override
    public String toString(){
        return production.toString() + ", " + lookAhead + "(" + curStr + ")";
    }
}