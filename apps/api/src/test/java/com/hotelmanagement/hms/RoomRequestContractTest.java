package com.hotelmanagement.hms;
import com.hotelmanagement.hms.room.dto.RoomRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
class RoomRequestContractTest {
 @Test void simplifiedRoomFormCanOmitInheritedFields(){
  var request=JsonMapper.builder().build().readValue("""
   {"roomTypeId":"11111111-1111-1111-1111-111111111111","code":"101","floor":"1","beds":2}
   """,RoomRequest.class);
  assertNull(request.adults());assertNull(request.children());assertNull(request.bedType());assertNull(request.nightlyRate());assertEquals(2,request.beds());
 }
}
