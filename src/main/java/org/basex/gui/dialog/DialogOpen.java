package org.basex.gui.dialog;

import static org.basex.core.Text.*;
import static org.basex.data.DataText.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.basex.core.Command;
import org.basex.core.Context;
import org.basex.core.cmd.AlterDB;
import org.basex.core.cmd.Copy;
import org.basex.core.cmd.CreateBackup;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.InfoDB;
import org.basex.core.cmd.List;
import org.basex.core.cmd.Restore;
import org.basex.data.MetaData;
import org.basex.gui.GUI;
import org.basex.gui.layout.BaseXBack;
import org.basex.gui.layout.BaseXButton;
import org.basex.gui.layout.BaseXEditor;
import org.basex.gui.layout.BaseXLabel;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXList;
import org.basex.io.in.DataInput;
import org.basex.util.Token;
import org.basex.util.Util;
import org.basex.util.list.ObjList;
import org.basex.util.list.StringList;

/**
 * Open database dialog.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class DialogOpen extends Dialog {
  /** List of currently available databases. */
  private final BaseXList choice;
  /** Information panel. */
  private final BaseXLabel doc;
  /** Information panel. */
  private final BaseXEditor detail;
  /** Buttons. */
  private final BaseXBack buttons;
  /** Rename button. */
  private final BaseXButton rename;
  /** Drop button. */
  private final BaseXButton drop;
  /** Open button. */
  private final BaseXButton open;
  /** Backup button. */
  private final BaseXButton backup;
  /** Restore button. */
  private final BaseXButton restore;
  /** Copy button. */
  private final BaseXButton copy;
  /** Manage flag. */
  private final boolean manage;
  /** Refresh. */
  private boolean refresh;

  /**
   * Default constructor.
   * @param main reference to the main window
   * @param m show manage dialog
   */
  public DialogOpen(final GUI main, final boolean m) {
    super(main, m ? MANAGETITLE : OPENTITLE);
    manage = m;
    // create database chooser
    final StringList db = List.list(main.context);

    choice = new BaseXList(db.toArray(), this, !m);
    set(choice, BorderLayout.CENTER);
    choice.setSize(160, 440);

    final BaseXBack info = new BaseXBack(new BorderLayout());
    info.setBorder(new CompoundBorder(new EtchedBorder(),
        new EmptyBorder(10, 10, 10, 10)));

    final Font f = choice.getFont();
    doc = new BaseXLabel(DIALOGINFO).border(0, 0, 5, 0);
    doc.setFont(f.deriveFont(f.getSize2D() + 7f));
    info.add(doc, BorderLayout.NORTH);

    detail = new BaseXEditor(false, this);
    detail.border(5, 5, 5, 5).setFont(f);

    BaseXLayout.setWidth(detail, 400);
    info.add(detail, BorderLayout.CENTER);

    final BaseXBack pp = new BaseXBack(new BorderLayout()).border(0, 12, 0, 0);
    pp.add(info, BorderLayout.CENTER);

    // create buttons
    final BaseXBack p = new BaseXBack(new BorderLayout());

    backup = new BaseXButton(BUTTONBACKUP, this);
    restore = new BaseXButton(BUTTONRESTORE, this);
    copy = new BaseXButton(BUTTONCOPY, this);
    rename = new BaseXButton(BUTTONRENAME, this);
    open = new BaseXButton(BUTTONOPEN, this);
    drop = new BaseXButton(BUTTONDROP, this);
    buttons = manage ?
        newButtons(this, drop, rename, copy, backup, restore, BUTTONOK) :
        newButtons(this, open, BUTTONCANCEL);
    p.add(buttons, BorderLayout.EAST);
    pp.add(p, BorderLayout.SOUTH);

    set(pp, BorderLayout.EAST);
    action(null);
    if(db.size() == 0) return;

    finish(null);
  }

  /**
   * Returns the name of the selected database.
   * @return database name
   */
  public String db() {
    return choice.getValue();
  }

  /**
   * Tests if no databases have been found.
   * @return result of check
   */
  public boolean nodb() {
    return choice.getList().length == 0;
  }

  @Override
  public void action(final Object cmp) {
    final Context ctx = gui.context;
    if(refresh) {
      // rebuild databases and focus list chooser
      choice.setData(List.list(ctx).toArray());
      choice.requestFocusInWindow();
      refresh = false;
    }

    final StringList dbs = choice.getValues();
    final String db = choice.getValue().trim();
    final ObjList<Command> cmds = new ObjList<Command>();
    boolean o = dbs.size() > 0;
    ok = manage || o;

    if(cmp == open) {
      close();
    } else if(cmp == drop) {
      if(!Dialog.confirm(gui, Util.info(DROPCONF, dbs.size()))) return;
      refresh = true;
      for(final String s : dbs) cmds.add(new DropDB(s));
    } else if(cmp == rename) {
      final DialogInput dr = new DialogInput(db, RENAMETITLE, gui, 1);
      if(!dr.ok() || dr.input().equals(db)) return;
      refresh = true;
      cmds.add(new AlterDB(db, dr.input()));
    } else if(cmp == copy) {
      final DialogInput dc = new DialogInput(db, COPYTITLE, gui, 2);
      if(!dc.ok() || dc.input().equals(db)) return;
      refresh = true;
      cmds.add(new Copy(db, dc.input()));
    } else if(cmp == backup) {
      for(final String s : dbs) cmds.add(new CreateBackup(s));
    } else if(cmp == restore) {
      for(final String s : dbs) cmds.add(new Restore(s));
    } else {
      // update components
      enableOK(buttons, BUTTONOPEN, o);
      enableOK(buttons, BUTTONBACKUP, o);
      enableOK(buttons, BUTTONDROP, o);
      o = ctx.mprop.dbexists(db);
      if(o) {
        // refresh info view
        DataInput in = null;
        final MetaData meta = new MetaData(db, ctx);
        try {
          in = new DataInput(meta.dbfile(DATAINF));
          meta.read(in);
          detail.setText(InfoDB.db(meta, true, true, true));
        } catch(final IOException ex) {
          detail.setText(Token.token(ex.getMessage()));
          o = manage;
        } finally {
          if(in != null) try { in.close(); } catch(final IOException ex) { }
        }
      }
      enableOK(buttons, BUTTONRENAME, o);
      enableOK(buttons, BUTTONCOPY, o);
      o = true;
      for(final String s : dbs) o &= Restore.list(s, ctx).size() != 0;
      enableOK(buttons, BUTTONRESTORE, o);
    }

    // run all commands
    if(cmds.size() != 0) {
      DialogProgress.execute(this, "", cmds.toArray(new Command[cmds.size()]));
    }
  }

  @Override
  public void close() {
    if(ok || choice.getValue().isEmpty()) dispose();
  }
}
