# LR1 Parser

## 写在前面

这是头歌平台上ECNU(SE)的编译原理与技术课程（实践）的项目。本实验为第三部分LR1语法分析器的设计。

虽然这是为特定课程项目编写的代码，但是代码具有通用性，稍作修改即可。

基本要求：自动分析器的设计。但是本人的目标是想在基本要求之上，实现LR1的分析表的自动构造。奈何个人能力确实太有限，在设计函数与debug时花费了太多时间，且此项目本人并没有在互联网上找到比较满意的答案作为参考，导致项目完成时已经超过了提交项目的DDL两天了，而这两天真的是没日没夜的debug却又不知道哪里有问题。得出的教训的就是要谨慎跨年级选课，人脉太少，很难找到能帮上忙的同学。

迟交的分数肯定不会高了，就绩点而言，一定是个悲剧。为了不让这个悲剧在我的同学以及来届同学中重演，我决定专门写一篇博客去分享我的思路，希望能帮大家节约时间。

## 任务描述

本关任务：根据给定文法，用java语言编写LR(1)语法分析器

### 相关知识

为了完成本关任务，你需要掌握：

1. LR文法
2. java 编程语言基础
3. C语言的基本结构知识

### LR分析器

在动手设计分析器之前，你应该先设计好下面文法的LR(1)分析表。

### Java

本实训涉及函数、结构体，标准流输入输出，字符串等操作

### 实验要求

实验文法定义

1. `program -> compoundstmt`
2. `stmt -> ifstmt | whilestmt | assgstmt | compoundstmt`
3. `compoundstmt -> { stmts }`
4. `stmts -> stmt stmts | E`
5. `ifstmt -> if ( boolexpr ) then stmt else stmt`
6. `whilestmt -> while ( boolexpr ) stmt`
7. `assgstmt -> ID = arithexpr ;`
8. `boolexpr -> arithexpr boolop arithexpr`
9. `boolop -> < | > | <= | >= | ==`
10. `arithexpr -> multexpr arithexprprime`
11. `arithexprprime -> + multexpr arithexprprime | - multexpr arithexprprime | E`
12. `multexpr -> simpleexpr multexprprime`
13. `multexprprime -> * simpleexpr multexprprime | / simpleexpr multexprprime | E`
14. `simpleexpr -> ID | NUM | ( arithexpr )`

### 起始符

program

### 保留字

1. `{ }`
2. `if ( ) then else`
3. `while ( )`
4. `ID = `
5. `> < >= <= ==`
6. `+ -`
7. `* /`
8. `ID NUM`
9. `E 是'空'`

### 分隔方式

同一行的输入字符用一个空格字符分隔，例如： ID = NUM ; 

### 错误处理

本实验需要考虑错误处理，如果程序不正确（包含语法错误），它应该打印语法错误消息（与行号一起），并且程序应该修正错误，并继续解析。
例如：

1. `语法错误,第4行,缺少";"`

### 输入

要求：在同一行中每个输入字符用一个空格字符分隔，无其余无关符号。

### 样例输入：

```
{  
ID = NUM ;  
}  
```

~~~
{ 
If E1 
then  
s1 
else 
If E2  
Then 
S2 
else  
S3 
}  
~~~

**并没有E1，E2等符号，这只是指代表达式**

### 输出

样例一输出

对于正确的程序，输出该程序的最右推导过程

对于有错误的的程序，输出错误问题并改正，继续输出正确的最右推导

每一组串之间均有一个空格符相隔开，分号，括号，=>符号前后均有一个空格符隔开，每一句推导只占一行

```
program =>   
compundstmt =>  
{ stmts } => 
{ stmt stmts } =>  
{ stmt } => 
{ assgstmt } =>   
{ ID = arithexpr ; } =>  
{ ID =  multexpr arithexprprime ; } =>  
{ ID = multexpr ; } =>  
{ ID = simpleexpr multexprprime ; } => 
{ ID = simpleexpr ; } =>  
{ ID = NUM ; }   
```

# 注意

* 和LL1一样，这里的保留字少了';'
* 采用书上的伪代码算法，在对E的处理时应该格外小心
* LR是右往左分析的，分析输出时应该注意
* 语法规则不含左递归。
* 我个人对于自己的语法错误处理的解决方案并不满意，所以在此**博客不含语法的错误处理**。

# 思路

来自教材*Compilers Principles, Techniques, and Tools, 2nd Ed. by Alfred V. Aho, Monica S. Lam, Ravi Sethi, Jeffrey D. Ullman (z-lib.org)*的472和473部分。中文版教材《编译原理 第二版》的算法4.53和算法4.56部分

* LR(1)的项集族的构造方法

<img src="LR1%20parser.assets/image-20220627182954326.png" alt="image-20220627182954326" style="zoom:80%;" />

* LR语法分析表的构造

<img src="LR1%20parser.assets/image-20220627182932527.png" alt="image-20220627182932527" style="zoom:80%;" />

# 代码

## Production类

```java
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
```

Production类的功能是存放产生式，其中father存放产生式的左边，sons存放的是产生式右边的字符串数组。所有的产生式都是最简的产生式。即右侧的'|'已经被拆分成了多条，这在语法规则的初始化部分得以处理。

重写了equals方法，这是方便之后的List判断是否contains。

重写了toString方法，这是方便debug的时候输出整洁。

## Item类

```java
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
```

Item类的功能是存放每一个项，以 **A -> B . a C, b**为例：

其中 A -> B a C 是一个产生式，用production属性记录。

'.'代表当前分析到哪一个地方了，其后紧跟着的a代表即将分析的字符串，用curStr表示，如果已经是产生式的尾巴则curStr为""。用curPointer指向production.sons的位置，指向的值和curStr相同，curStr个人只是为了判断方便而记录的一个冗余信息。

','之后的字符串b代表lookahead，用变量lookAhead记录。这里不是集合，而是一个字符串，这里的处理方式是，若一个产生式的lookahead有多个，则生成多个Item。每个Item只有一个lookahead

重写equals方法，用于判断是否相等，List是否contains。

重写toString方法，用于方便debug的时候输出。

## 静态变量

```java
private static List<Production> productions = new ArrayList<>();//所有产生式的集合
private static List<String> terminals = new ArrayList<>(); //终止符的集合
private static List<String> nonTerminals = new ArrayList<>(); //非终止符的集合
private static Map<String, Set<String>> FirstSets = new HashMap<>(); //所有符号的first集

private static List<List<Item>> C = new ArrayList<>(); //集族

private static String[][] ACTION;
private static Integer[][] GOTO;

private static Stack<Production> outputStack = new Stack<>();
```

* C代表集族，里面的每一个List\<Item>都是一个项集，代表一个完整的状态。
* ACTION表里面的字符串采用"s_12", "r_15"这样的操作+下划线+数字的形式记录信息。分别代表shift 12 状态以及用15号产生式进行规约。用字符串存储这些纯粹是为了方便，个人认为更合适的方法应该单独编写一个operation 对象。
* GOTO表里面存储的是goto后的状态的值。
* outputStack是用栈存储了产生式的规约顺序，最后输出的时候再利用这些进行产生式的顺序信息进行处理。

## 文法初始化

```java
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
```

* 为了方便，我将文法的字符串拆开了，懒得处理'|'了，当然也可以自己再写一个函数去将这些产生式拆开成最简单的单元。
* 每个产生式的字符串先识别"->"拆成两部分，左边就是production.father，右边再继续split("\\\\s+")去产生一个字符串数组，即production.sons
* 每个产生式的father就是一个非终结符，所有的文法也就包含了所有的非终结符信息，所以此处构造了nonTerminals集合。

## 非终结符和终结符的判断

```java
private static void initTerminal(){
    String reservedWord = "{ } if ( ) then else while ( ) ID = > < >= <= ==  + -  * / ID NUM  E $  ; ";
    terminals = Arrays.asList(reservedWord.split("\\s+"));
}

private static boolean isTerminal(String word){
    return terminals.contains(word);
}

private static boolean isNonTerminal(String word){
    return nonTerminals.contains(word);
}
```

* 构造了非终结符，原题目里面没有';'是错的，别被坑了。此处还自己加了个dollar符方便处理。
* isTerminal和isNonTerminal函数都是为了方便。

## 初始化First集

```java
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
```

* initFirst函数中首先将所有的非终结符的first初始化，因为他们的first就是自身。
* initFrist函数再遍历nonTerminals，给每个非终结符调用一次constructFirst去构造它的first集。
* constructFirst中，如果要构造的这个非终结符已经在FirstSets中有数据了，就不用无效果地再走一次函数了，直接返回。这样可以提高递归效率。
* X的First集为fatherFirst，如果FirstSets里面有这个X的数据，就从里面得到数据，如果没有就new一个Set。
* 遍历所有的father为X的产生式，得到这些产生式的sons，从左到右遍历sons的每个son，先给son构造一次First，此时FirstSets里面一定有son的first了，再从中得到son的first并加到X的fatherFirst里面。如果son的First集里面还有空(E)，就继续追加下一个son的first集到fatherFirst里面，直到没有E就可以终止sons的遍历。
* 跳出for循环后，将fatherFirst加到FirstSets里面，至此X的first集构造完成。

## 构造集族

```java
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
```

书上的思路是先讲闭包，再将goto，最后再来构造集族。但是由下到上的讲解不直观。所以我先讲构造集族，再goto，最后闭包。这些思路完全按照课本走的，唯一的难点就是真的下定决心去实现。

* 首先写出扩展文法，即**program' -> program**，并且他的项应该是**program -> . program , $**，代表整个程序分析的开始。最后将此项放到initSet里求闭包，得到的项集结果放到集族C里，这就是状态0。
* 下一步就是给每一个项去判断他的goto，所以需要遍历所有的符号。这里allSymbol存储了所有的终结符和非终结符。
* 在伪代码的思路中，经常可以看到until no more XXX，表示一个集合的元素不再增加，就终止循环。此处我就采用了while(true)，初始化一个lastLen代表集族的上一个状态的大小，在while循环的最后来看这一番操作之后集族的大小newLen是否增加，如果状态已经不变则break跳出循环。
* 遍历集族的每一个项集，这些地方我都用的是for而不是迭代器，是因为在遍历的时候会对原来的List进行add，报异常。如果用for的话，每次增加的元素都在末尾，就没事儿。这是java的一些性质，个人没有深究。（刷八股文的时候再来看）
* 给项集I_i的每一项都对每一个符号判断一次goto后的项集I_j，如果不为空且原来的集族不含此I_j，就将集族C增添一个项集I_j，即新增一个状态。
* 这里判断集族中是否含有此状态，用的是C.contains(I_j)，是因为contains判断的是集族中的某个项集List\<Item>是否equals(I_J)，而List.equals是自动重写了equals方法，判断两个List长度相同，且每个元素相等且顺序相同则相等。而判断元素Item的equals我们已经重写过了。这就实现了判断集族中是否包含I_j。

## goto

```java
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
```

* 从项集I，需要goto经过一个X。
* 初始化一个空的项集J，遍历每个curStr为X的项，这些项能够实现经过X的goto。goto后的新的状态就是将'.'移到下一个位置，即curPointer+1，curStr也更新成下一个字符串。如果下一个位置已经是某项的尾巴，则说明curPointer越界，curStr用""表示。lookahead赋值为本来的lookahead。将这些经过goto的newItem放到J里。当项集中的每个项都遍历完之后，这个项集的goto后的项就确定了。
* 最后返回J的闭包，表示经过X的完整的状态。

## 闭包

```java
private static List<Item> clousure(List<Item> I ){
    int lastLen = I.size();
    while(true){
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
```

* 遍历项集的每一项，找到它即将分析的curStr，这里和教材的变量名保持一致用B来表示，再从所有的产生式中找到从B开始能够衍生的所有的产生式。
* 这些产生式，从curStr之后的位置开始，一致到lookahead，构成一串$$\beta a$$，这里命名为betaA。计算betaA的first集，思路同初始化first的思路一样。因为已经构造好了所有的符号的first存放在FirstSets里，所以直接在里面拿取即可，如果有E则继续追加。最后得到betaA的first。
* 找到first中的每个终结符b，新增一个项，此项与项集I遍历的项的产生式一样，lookahead为b，curPointer指向sons的开始。
* 不断循环完善闭包，不再增加时闭包构造完成。

## Parsing Table

至此已经构造好了集族，里面的每一个状态都已知晓，现在可以直接构造分析表了。

```java
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
```

* 分为ACTION表和GOTO表，高度为集族的状态数，ACTION的宽度为非终结符的数量，GOTO的宽度为终结符的数量。
* 遍历每个项集（状态），终结符就shift，在尾巴就reduce，非终结符就GOTO，再注意一下accept的情况。这些判断在教材LR语法分析表的构造部分写的已经很详细了。

## 解析

```java
private static void parse(){
    Stack<String> symbleStack = new Stack<>();
    Stack<Integer> stateStack = new Stack<>();
    stateStack.push(0);
    String[] progStr = prog.toString().trim().split("\\s+");

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
```

* 这个地方我分为了符号栈和状态栈，分析的过程是大家最为熟悉的地方，不在此赘述。
* 值得注意的地方是对空(E)的处理，如果ACTION表在某状态遇到了某个非终结符找不到对应的操作，则应该考虑此次是一次空的转换，假设遇到了E，此时p指针应该回退一个，重新指向这个终结符，再将E进行压栈处理。这样就完成了对E的操作。这也是教材未提及的地方，是我找了两天bug的坑。但是我觉得这种遇到E的操作并不好，应该有更好的办法。

## 输出

```java
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
```

* 在进行解析时，将产生式规约的顺序已经记录在了outputStack中，只要不断地pop栈，就知道应该怎么样从一个初始的program逐渐构造成输入的程序。
* 此处一定要注意，由于LR，所以在解析时应该从右往左替换非终结符，例如 A -> B B时应该先替换右边的B。所以该处的for循环的i是output.size()-1到0。替换时找到指定的非终结符的位置i，同样j是从sons的最后一个位置sons.length-1遍历到0，不断在此处插入sons[j]即可。遇到空则忽略操作。
* 此时项目已经基本完成了，再次提醒此处**不含错误处理**。错误处理需要看官自己实现。

## 其他

```java
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
```

题干部分的代码以及初始化代码。
