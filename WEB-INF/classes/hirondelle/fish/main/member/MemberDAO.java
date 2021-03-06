package hirondelle.fish.main.member;

import static hirondelle.fish.main.member.MemberAction.COUNT_ACTIVE_MEMBERS;
import static hirondelle.fish.main.member.MemberAction.MEMBER_ADD;
import static hirondelle.fish.main.member.MemberAction.MEMBER_CHANGE;
import static hirondelle.fish.main.member.MemberAction.MEMBER_DELETE;
import static hirondelle.fish.main.member.MemberAction.MEMBER_FETCH;
import static hirondelle.fish.main.member.MemberAction.MEMBER_LIST;
import hirondelle.fish.test.doubles.FakeDAOBehavior;
import hirondelle.web4j.database.DAOException;
import hirondelle.web4j.database.Db;
import hirondelle.web4j.database.DuplicateException;
import hirondelle.web4j.database.ForeignKeyException;
import hirondelle.web4j.model.Id;
import hirondelle.web4j.util.Util;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/** 
 Data Access Object (DAO) for {@link Member} objects.
 
 <P><b>Design Note</b><br> 
 <P>This class is not final. This allows creating {@link MemberDAOFake} as a subclass.
 Instead of using a subclass to define the fake DAO, some might prefer to define  
 an interface for the DAO instead, along with two concrete implementations, 
 one for the real DAO, and one for the fake DAO. 
 
 <P>The subclass style was used here since it seems a bit more compact : only 
 two top-level items need to be defined, not three.
 
 <P>Either style will work.
*/
public class MemberDAO {

  /**
   Return either a real or a fake DAO.
   
   <P>The underlying mechanism for swapping between the two implementations 
   relies on a simple <tt>System</tt> property.  
  */
  public static MemberDAO getInstance(){
    MemberDAO result = null;
    String environmentSetting = System.getProperty(FakeDAOBehavior.USE_FAKE_DAOS);
    if( Boolean.TRUE.toString().equalsIgnoreCase(environmentSetting) ) {
      fLogger.fine("Using fake Member DAO.");
      result = new MemberDAOFake();
    }
    else {
      fLogger.fine("Using real Member DAO." );
      result = new MemberDAO();
    }
    return result;
  }
  
  /**
   Return a <tt>List</tt> of all {@link Member} objects, sorted by Member Name.
  */
  List<Member> list() throws DAOException {
    return Db.list(Member.class, MEMBER_LIST);
  }
   
  /**
   Return a single {@link Member} identified by its id.
  */
  public Member fetch(Id aMemberId) throws DAOException {
    return Db.fetch(Member.class, MEMBER_FETCH, aMemberId);
  }
   
  /**
   Add a new {@link Member} to the database.
   
   <P>The name of the <tt>Member</tt> must be unique. If there is a name 
   conflict, then a {@link DuplicateException} is thrown.
   
   @return the autogenerated database id.
  */
  Id add(Member aMember) throws DAOException, DuplicateException {
    return Db.add(MEMBER_ADD, baseParamsFrom(aMember));
  }
   
  /**
   Update an existing {@link Member}.
    
   <P>The name of the <tt>Member</tt> must be unique. If there is a name 
   conflict, then a {@link DuplicateException} is thrown.
   
   @return <tt>true</tt> only if a record is actually edited. 
  */
  boolean change(Member aMember) throws DAOException, DuplicateException {
    Object[] params = Db.addIdTo(baseParamsFrom(aMember), aMember.getId());
    return Util.isSuccess(Db.edit(MEMBER_CHANGE, params));
  }
  
  /**
   Delete a single {@link Member}.
   
   <P>If an item is linked to this {@link Member}, then deletion will fail, and a 
   {@link DAOException} is thrown.
  */
  int delete(Id aMemberId) throws DAOException {
     return Db.delete(MEMBER_DELETE, aMemberId);
  }
   
  /**
   Delete one or more {@link Member}s in one operation. 
   
   <P>Returns the total number of deleted items.
   
   <P><i>This method has an unusual policy regarding failed operations.</i> 
   Each delete operation is treated as a <i>separate</i> item. 
   No transactions are used here. 
   If one delete operation fails (for example, because of a foreign key constraint), 
   then this method will not stop. Rather, it will keep trying to 
   delete items in sequence, until it gets to the end of the given Member ids.
   
   <P>It is important to be aware of this policy. You may implement in another style, if 
   desired.  It's highly recommended that the calling action examine the return value of 
   this method, and compare it with the number of Member Ids, to detect if an 
   error has occurred.
   
   <P>(An interesting alternative policy would be to return not a single integer, 
   but the list of Ids of items that were successfully (or unsuccessfully) deleted.)
  */
  int deleteMany(Collection<Id> aMemberIds) throws DAOException {
    int result = 0;
    for(Id id: aMemberIds){
      try {
        result = result +  Db.delete(MEMBER_DELETE, id);
      }
      catch(ForeignKeyException ex){
        fLogger.fine(
          "Foreign key constraint violated. Cannot delete Member with id : " + 
          id + " Root cause : " + ex.getCause()
        );
      }
    }
    return result;
  }
  
  /** Return the number of currently active members.  */
  Integer getNumActiveMembers() throws DAOException {
    return Db.fetchValue(Integer.class, COUNT_ACTIVE_MEMBERS);
  }
  
  // PRIVATE //
  private static final Logger fLogger = Util.getLogger(MemberDAO.class);
  
  /*
   Small defect: would like to declare a private constructor here, such that the 
   caller is forced to use the factory method. But that can't be done, since  
   the constructor needs to be visible from the 'fake' subclass.
  */

  private Object[] baseParamsFrom(Member aMember){
    return new Object[]{
      aMember.getName(), aMember.getIsActive(), aMember.getDisposition().getId()
    };
  }
}
