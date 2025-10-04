package assignment2;

import java.util.*;

class IdGen {
    private static int id = 1000;
    public static synchronized int next() { return id++; }
}

abstract class BaseEntity {
    protected int id;
    protected String name;
    public BaseEntity(String name) { this.id = IdGen.next(); this.name = name; }
    public int getId() { return id; }
    public String getName() { return name; }
}

class Supplier extends BaseEntity {
    private String contact;
    public Supplier(String name, String contact) { super(name); this.contact = contact; }
    public String getContact() { return contact; }
    public String toString() { return String.format("Supplier[%d] %s (contact: %s)", id, name, contact); }
}

class Item extends BaseEntity {
    private String unit;
    public Item(String name, String unit) { super(name); this.unit = unit; }
    public String getUnit() { return unit; }
    public String toString() { return String.format("Item[%d] %s (%s)", id, name, unit); }
}

interface Reportable { void printSummary(); }

class PurchaseOrder implements Reportable {
    private int id;
    private Supplier supplier;
    private Date date;
    private List<PurchaseOrderLine> lines = new ArrayList<>();
    public PurchaseOrder(Supplier supplier) { this.id = IdGen.next(); this.supplier = supplier; this.date = new Date(); }
    public int getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public List<PurchaseOrderLine> getLines() { return lines; }
    public void addLine(Item item, int qty) { if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive"); lines.add(new PurchaseOrderLine(lines.size()+1, this, item, qty)); }
    public PurchaseOrderLine findLineByNo(int lineNo) { for (PurchaseOrderLine l : lines) if (l.getLineNo() == lineNo) return l; return null; }
    public void printSummary() { System.out.println("--- Purchase Order " + id + " (Supplier: " + supplier.getName() + ") ---"); for (PurchaseOrderLine l : lines) System.out.println(l); }
}

class PurchaseOrderLine {
    private int lineNo;
    private PurchaseOrder po;
    private Item item;
    private int orderedQty;
    private int receivedQty = 0;
    public PurchaseOrderLine(int lineNo, PurchaseOrder po, Item item, int orderedQty) { this.lineNo = lineNo; this.po = po; this.item = item; this.orderedQty = orderedQty; }
    public int getLineNo() { return lineNo; }
    public Item getItem() { return item; }
    public int getOrderedQty() { return orderedQty; }
    public int getReceivedQty() { return receivedQty; }
    public int getOutstandingQty() { return orderedQty - receivedQty; }
    public void addAcceptedQty(int q) { if (q < 0) throw new IllegalArgumentException("Quantity cannot be negative"); int allowed = Math.min(q, getOutstandingQty()); receivedQty += allowed; }
    public String toString() { return String.format("Line %d: %s | Ordered: %d | Accepted: %d | Outstanding: %d", lineNo, item.getName(), orderedQty, receivedQty, getOutstandingQty()); }
}

class GoodsReceipt implements Reportable {
    private int id;
    private PurchaseOrder po;
    private Date date;
    private Map<Integer, Integer> receivedByLine = new HashMap<>();
    public GoodsReceipt(PurchaseOrder po) { this.id = IdGen.next(); this.po = po; this.date = new Date(); }
    public int getId() { return id; }
    public PurchaseOrder getPurchaseOrder() { return po; }
    public void receiveLine(int lineNo, int qty) { if (qty < 0) throw new IllegalArgumentException("Received qty can't be negative"); PurchaseOrderLine pol = po.findLineByNo(lineNo); if (pol == null) throw new IllegalArgumentException("Invalid PO line no: " + lineNo); int allowed = Math.min(qty, pol.getOutstandingQty()); receivedByLine.put(lineNo, receivedByLine.getOrDefault(lineNo, 0) + allowed); }
    public Map<Integer,Integer> getReceivedByLine() { return receivedByLine; }
    public void printSummary() { System.out.println("--- Goods Receipt " + id + " for PO " + po.getId() + " ---"); for (Map.Entry<Integer,Integer> e : receivedByLine.entrySet()) { PurchaseOrderLine l = po.findLineByNo(e.getKey()); System.out.println("PO Line " + e.getKey() + ": Item=" + l.getItem().getName() + " ReceivedQty=" + e.getValue()); } }
}

class InspectionLot implements Reportable {
    private int id;
    private GoodsReceipt gr;
    private Map<Integer,Integer> acceptedByLine = new HashMap<>();
    private Map<Integer,String> rejectionReasonByLine = new HashMap<>();
    private Map<Integer,Integer> rejectedByLine = new HashMap<>();
    public InspectionLot(GoodsReceipt gr) { this.id = IdGen.next(); this.gr = gr; }
    public int getId() { return id; }
    public GoodsReceipt getGoodsReceipt() { return gr; }
    public void acceptLine(int lineNo, int qty) { int received = gr.getReceivedByLine().getOrDefault(lineNo, 0); int alreadyAccepted = acceptedByLine.getOrDefault(lineNo, 0); int allowed = Math.min(qty, received - alreadyAccepted); if (allowed < 0) allowed = 0; acceptedByLine.put(lineNo, alreadyAccepted + allowed); }
    public void rejectLine(int lineNo, int qty, String reason) { int received = gr.getReceivedByLine().getOrDefault(lineNo, 0); int alreadyRejected = rejectedByLine.getOrDefault(lineNo, 0); int allowed = Math.min(qty, received - alreadyRejected - acceptedByLine.getOrDefault(lineNo,0)); if (allowed < 0) allowed = 0; rejectedByLine.put(lineNo, alreadyRejected + allowed); rejectionReasonByLine.put(lineNo, reason); }
    public Map<Integer,Integer> getAcceptedByLine() { return acceptedByLine; }
    public Map<Integer,Integer> getRejectedByLine() { return rejectedByLine; }
    public Map<Integer,String> getRejectionReasonByLine() { return rejectionReasonByLine; }
    public void printSummary() { System.out.println("--- Inspection Lot " + id + " for GR " + gr.getId() + " ---"); for (Map.Entry<Integer,Integer> e : gr.getReceivedByLine().entrySet()) {
            int line = e.getKey(); int rec = e.getValue(); int acc = acceptedByLine.getOrDefault(line,0); int rej = rejectedByLine.getOrDefault(line,0); String reason = rejectionReasonByLine.getOrDefault(line, ""); PurchaseOrderLine pol = gr.getPurchaseOrder().findLineByNo(line); System.out.println("PO Line " + line + ": Item=" + pol.getItem().getName() + " Received=" + rec + " Accepted=" + acc + " Rejected=" + rej + (reason.isEmpty()?"":" Reason=" + reason));
        }
    }
}

class Rejection {
    private int id;
    private PurchaseOrderLine poLine;
    private int qty;
    private String reason;
    public Rejection(PurchaseOrderLine poLine, int qty, String reason) { this.id = IdGen.next(); this.poLine = poLine; this.qty = qty; this.reason = reason; }
    public String toString() { return String.format("Rejection[%d] POLine:%d Item:%s Qty:%d Reason:%s", id, poLine.getLineNo(), poLine.getItem().getName(), qty, reason); }
}

class Inventory {
    private Map<Integer,Integer> stock = new HashMap<>();
    public void addStock(Item item, int qty) { stock.put(item.getId(), stock.getOrDefault(item.getId(), 0) + qty); }
    public int getStock(Item item) { return stock.getOrDefault(item.getId(), 0); }
    public void printInventory(List<Item> items) { System.out.println("--- Inventory Summary ---"); for (Item it : items) System.out.println(it + " Stock=" + getStock(it)); }
}

public class FoodSupplyApp {
    private static Scanner sc = new Scanner(System.in);
    private static List<Supplier> suppliers = new ArrayList<>();
    private static List<Item> items = new ArrayList<>();
    private static List<PurchaseOrder> pos = new ArrayList<>();
    private static List<GoodsReceipt> grs = new ArrayList<>();
    private static List<InspectionLot> inspections = new ArrayList<>();
    private static List<Rejection> rejections = new ArrayList<>();
    private static Inventory inventory = new Inventory();

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n 1 Add Supplier \n 2 Add Item\n 3 Create Purchase Order\n 4 Receive Goods\n 5 Create Inspection\n 6 Record Acceptance/Rejection\n 7 Inventory Summary\n 8 Exit\n");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1": addSupplier(); break;
                    case "2": addItem(); break;
                    case "3": createPO(); break;
                    case "4": receiveGoods(); break;
                    case "5": createInspection(); break;
                    case "6": recordAcceptanceRejection(); break;
                    case "7": inventorySummary(); break;
                    case "8": System.out.println("Exiting"); return;
                    default: System.out.println("Invalid option");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private static void addSupplier() {
        System.out.print("Supplier name: "); String name = sc.nextLine().trim(); if (name.isEmpty()) { System.out.println("Name required"); return; }
        System.out.print("Contact: "); String contact = sc.nextLine().trim(); Supplier s = new Supplier(name, contact); suppliers.add(s); System.out.println("Added " + s);
    }

    private static void addItem() {
        System.out.print("Item name: "); String name = sc.nextLine().trim(); if (name.isEmpty()) { System.out.println("Name required"); return; }
        System.out.print("Unit (kg/pcs/ltr): "); String unit = sc.nextLine().trim(); Item it = new Item(name, unit); items.add(it); System.out.println("Added " + it);
    }

    private static Supplier findSupplierById(int id) { for (Supplier s : suppliers) if (s.getId() == id) return s; return null; }
    private static Item findItemById(int id) { for (Item it : items) if (it.getId() == id) return it; return null; }
    private static PurchaseOrder findPOById(int id) { for (PurchaseOrder p : pos) if (p.getId() == id) return p; return null; }
    private static GoodsReceipt findGRById(int id) { for (GoodsReceipt g : grs) if (g.getId() == id) return g; return null; }
    private static InspectionLot findInspectionById(int id) { for (InspectionLot i : inspections) if (i.getId() == id) return i; return null; }

    private static void createPO() {
        if (suppliers.isEmpty()) { System.out.println("No suppliers"); return; }
        System.out.println("Suppliers:"); for (Supplier s : suppliers) System.out.println(s.getId() + " " + s.getName());
        System.out.print("Enter supplier id: "); int sid = readInt(); Supplier s = findSupplierById(sid); if (s == null) { System.out.println("Invalid supplier"); return; }
        PurchaseOrder po = new PurchaseOrder(s);
        while (true) {
            if (items.isEmpty()) { System.out.println("No items available. Add items first."); break; }
            System.out.println("Items:"); for (Item it : items) System.out.println(it.getId() + " " + it.getName());
            System.out.print("Enter item id for PO line (or 0 to finish): "); int iid = readInt(); if (iid == 0) break; Item it = findItemById(iid); if (it == null) { System.out.println("Invalid item"); continue; }
            System.out.print("Enter qty: "); int qty = readInt(); if (qty <= 0) { System.out.println("Qty must be >0"); continue; }
            po.addLine(it, qty);
        }
        pos.add(po);
        System.out.println("Created PO " + po.getId());
        po.printSummary();
    }

    private static void receiveGoods() {
        if (pos.isEmpty()) { System.out.println("No POs"); return; }
        System.out.println("POs:"); for (PurchaseOrder p : pos) System.out.println(p.getId() + " Supplier:" + p.getSupplier().getName());
        System.out.print("Enter PO id to receive: "); int pid = readInt(); PurchaseOrder po = findPOById(pid); if (po == null) { System.out.println("Invalid PO"); return; }
        GoodsReceipt gr = new GoodsReceipt(po);
        while (true) {
            for (PurchaseOrderLine l : po.getLines()) System.out.println("Line " + l.getLineNo() + " Item:" + l.getItem().getName() + " Outstanding:" + l.getOutstandingQty());
            System.out.print("Enter line no to record received (0 to finish): "); int ln = readInt(); if (ln == 0) break; PurchaseOrderLine pol = po.findLineByNo(ln); if (pol == null) { System.out.println("Invalid line"); continue; }
            System.out.print("Enter received qty: "); int rq = readInt(); gr.receiveLine(ln, rq);
        }
        grs.add(gr);
        System.out.println("Recorded GR " + gr.getId());
        gr.printSummary();
    }

    private static void createInspection() {
        if (grs.isEmpty()) { System.out.println("No goods receipts"); return; }
        System.out.println("GRs:"); for (GoodsReceipt g : grs) System.out.println(g.getId() + " for PO " + g.getPurchaseOrder().getId());
        System.out.print("Enter GR id to inspect: "); int gid = readInt(); GoodsReceipt gr = findGRById(gid); if (gr == null) { System.out.println("Invalid GR"); return; }
        InspectionLot il = new InspectionLot(gr);
        inspections.add(il);
        System.out.println("Created Inspection Lot " + il.getId());
        il.printSummary();
    }

    private static void recordAcceptanceRejection() {
        if (inspections.isEmpty()) { System.out.println("No inspections"); return; }
        System.out.println("Inspections:"); for (InspectionLot i : inspections) System.out.println(i.getId() + " GR:" + i.getGoodsReceipt().getId());
        System.out.print("Enter Inspection id: "); int iid = readInt(); InspectionLot il = findInspectionById(iid); if (il == null) { System.out.println("Invalid inspection"); return; }
        GoodsReceipt gr = il.getGoodsReceipt();
        while (true) {
            for (Map.Entry<Integer,Integer> e : gr.getReceivedByLine().entrySet()) {
                PurchaseOrderLine pol = gr.getPurchaseOrder().findLineByNo(e.getKey());
                System.out.println("Line " + e.getKey() + " Item:" + pol.getItem().getName() + " Received:" + e.getValue());
            }
            System.out.print("Enter line no to accept/reject (0 to finish): "); int ln = readInt(); if (ln == 0) break; System.out.print("A to accept R to reject: "); String ar = sc.nextLine().trim().toUpperCase(); if (ar.equals("A")) { System.out.print("Enter qty to accept: "); int q = readInt(); il.acceptLine(ln, q); PurchaseOrderLine pol = gr.getPurchaseOrder().findLineByNo(ln); inventory.addStock(pol.getItem(), Math.min(q, q)); pol.addAcceptedQty(q); }
            else if (ar.equals("R")) { System.out.print("Enter qty to reject: "); int q = readInt(); System.out.print("Reason: "); String reason = sc.nextLine().trim(); il.rejectLine(ln, q, reason); PurchaseOrderLine pol = gr.getPurchaseOrder().findLineByNo(ln); rejections.add(new Rejection(pol, q, reason)); }
            else System.out.println("Invalid choice");
        }
        il.printSummary();
    }

    private static void inventorySummary() {
        inventory.printInventory(items);
        System.out.println("--- Outstanding PO Quantities ---");
        for (PurchaseOrder p : pos) { System.out.println("PO " + p.getId() + " Supplier:" + p.getSupplier().getName()); for (PurchaseOrderLine l : p.getLines()) System.out.println(l); }
    }

    private static int readInt() {
        try { String s = sc.nextLine().trim(); return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
