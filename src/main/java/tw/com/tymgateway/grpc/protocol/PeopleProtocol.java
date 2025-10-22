package tw.com.tymgateway.grpc.protocol;

/**
 * Gateway專用的People協議類型定義
 * 簡化的協議定義，用於gRPC通訊，不依賴backend
 */
public class PeopleProtocol {

    // 請求類型
    public static class GetAllPeopleRequest {}

    public static class GetPeopleByNameRequest {
        private String name;

        public GetPeopleByNameRequest() {}

        public GetPeopleByNameRequest(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class InsertPeopleRequest {
        private tw.com.tymgateway.dto.PeopleData peopleData;

        public InsertPeopleRequest() {}

        public InsertPeopleRequest(tw.com.tymgateway.dto.PeopleData peopleData) {
            this.peopleData = peopleData;
        }

        public tw.com.tymgateway.dto.PeopleData getPeopleData() { return peopleData; }
        public void setPeopleData(tw.com.tymgateway.dto.PeopleData peopleData) { this.peopleData = peopleData; }
    }

    public static class UpdatePeopleRequest {
        private String name;
        private tw.com.tymgateway.dto.PeopleData peopleData;

        public UpdatePeopleRequest() {}

        public UpdatePeopleRequest(String name, tw.com.tymgateway.dto.PeopleData peopleData) {
            this.name = name;
            this.peopleData = peopleData;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public tw.com.tymgateway.dto.PeopleData getPeopleData() { return peopleData; }
        public void setPeopleData(tw.com.tymgateway.dto.PeopleData peopleData) { this.peopleData = peopleData; }
    }

    public static class DeletePeopleRequest {
        private String name;

        public DeletePeopleRequest() {}

        public DeletePeopleRequest(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // 響應類型
    public static class GetAllPeopleResponse {
        private java.util.List<tw.com.tymgateway.dto.PeopleData> people;

        public GetAllPeopleResponse() {}

        public java.util.List<tw.com.tymgateway.dto.PeopleData> getPeople() { return people; }
        public void setPeople(java.util.List<tw.com.tymgateway.dto.PeopleData> people) { this.people = people; }
    }

    public static class PeopleResponse {
        private boolean success;
        private String message;
        private tw.com.tymgateway.dto.PeopleData people;

        public PeopleResponse() {}

        public boolean getSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public tw.com.tymgateway.dto.PeopleData getPeople() { return people; }
        public void setPeople(tw.com.tymgateway.dto.PeopleData people) { this.people = people; }

        public boolean hasPeople() { return people != null; }
    }

    public static class DeletePeopleResponse {
        private boolean success;
        private String message;

        public DeletePeopleResponse() {}

        public boolean getSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
