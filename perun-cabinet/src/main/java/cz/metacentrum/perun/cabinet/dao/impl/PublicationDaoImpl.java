package cz.metacentrum.perun.cabinet.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.metacentrum.perun.cabinet.dao.IPublicationDao;
import cz.metacentrum.perun.cabinet.dao.mybatis.PublicationExample;
import cz.metacentrum.perun.cabinet.dao.mybatis.PublicationMapper;
import cz.metacentrum.perun.cabinet.model.Publication;
import cz.metacentrum.perun.cabinet.model.PublicationForGUI;
import cz.metacentrum.perun.cabinet.service.SortParam;

/**
 * Class of DAO layer for handling Publication entity. 
 * Provides connection to proper mapper.
 *
 * @author Jiri Harazim <harazim@mail.muni.cz>
 * @author Pavel Zlamal <256627@mail.muni.cz>
 */
public class PublicationDaoImpl implements IPublicationDao {

	private static final String DESC = "DESC";
	private static final String ASC = "ASC";
	private PublicationMapper publicationMapper;

	// setters ----------------------
	
	public void setPublicationMapper(PublicationMapper publicationMapper) {
		this.publicationMapper = publicationMapper;
	}

	// methods ----------------------
	
	public int createPublication(Publication p) {
		publicationMapper.insert(p);
		return p.getId();
	}
	

	public int createInternalPublication(Publication p) {
		publicationMapper.insertInternal(p);
		return p.getId();
	}

	
	public List<Publication> findPublicationsByFilter(Publication p) {
		return publicationMapper.selectByFilter(p);
	}


	public Publication findPublicationById(Integer publicationId) {
		return publicationMapper.selectByPrimaryKey(publicationId);
	}
	
	public List<Publication> findAllPublications() {
		PublicationExample example = new PublicationExample();
		return publicationMapper.selectByExample(example);
	}

	public List<PublicationForGUI> findRichPublicationsByGUIFilter(Publication p, Integer userId, int yearSince, int yearTill) {

		PublicationExample example = new PublicationExample();
		boolean exampleUsed = false;

		if (p != null) {
			// if publication not null process it's params

			if (p.getId() != null && p.getId() != 0) {
				example.add().andIdEqualTo(p.getId());
				exampleUsed = true;
			}
			if (p.getYear() != null && p.getYear() != 0) {
				example.add().andYearEqualTo(p.getYear());
				exampleUsed = true;
			}
			if (p.getCategoryId() != null && p.getCategoryId() != 0) {
				example.add().andCategoryIdEqualTo(p.getCategoryId());
				exampleUsed = true;
			}
			if (p.getTitle() != null && p.getTitle() != "") {
				// must call Rich because SQL query must contains "P." prefix inside function LOWER()
				example.add().andRichTitleContains(p.getTitle().toLowerCase());
				exampleUsed = true;
			}
			if (p.getIsbn() != null && p.getIsbn() != "") {
				// must call Rich because SQL query must contains "P." prefix inside function LOWER()
				example.add().andRichIsbnContains(p.getIsbn().toLowerCase());
				exampleUsed = true;
			}
			if (p.getRank() != null) {
				example.add().andRankEqualTo(p.getRank());
				exampleUsed = true;
			}
			if (p.getDoi() != null) {
				example.add().andRichDoiContains(p.getDoi().toLowerCase());
				exampleUsed = true;
			}
			if (p.getLocked() != null) {
				example.add().andLockedEqualTo(p.getLocked());
				exampleUsed = true;
			}
			
		}
		// process rest of params
		if (yearSince != 0) {
			example.add().andYearGreaterThanOrEqualTo(yearSince);
			exampleUsed = true;
		}
		if (yearTill != 0) {
			example.add().andYearLessThanOrEqualTo(yearTill);
			exampleUsed = true;
		}
	
		/*
		 * We !MUST! check if 'example' was really used.
		 * Because it can be 'not null' but with no property set 
		 * => therefore bad SQL would be generated by PublicationMapper.xml
		 */
		if (exampleUsed) {
			// use AND example clause with example
			return publicationMapper.selectRichByANDExample(example, userId);	
		} else {
			// use AND example clause without example
			return publicationMapper.selectRichByANDExample(null, userId);
		}
		
	}

	public PublicationForGUI findRichPublicationById(Integer publicationId) {
		return publicationMapper.selectRichByPrimaryKey(publicationId);
	}
	
	public List<PublicationForGUI> findRichPublicationsByFilter(Publication p, Integer userId) {
		return publicationMapper.selectRichByFilter(p, userId);
	}
	
	public List<Publication> findPublicationsByFilter(Publication p,
			SortParam sp) {
		Map<String, Object> params = new HashMap<String,Object>();
		params.put("id", p.getId());
		params.put("categoryId", p.getCategoryId());
		params.put("externalId", p.getExternalId());
		params.put("isbn", p.getIsbn());
		params.put("main", p.getMain());
		params.put("createdBy", p.getCreatedBy());
		params.put("createdDate", p.getCreatedDate());
		params.put("publicationSystemId", p.getPublicationSystemId());
		params.put("title", p.getTitle());
		params.put("year", p.getYear());
		params.put("rank", p.getRank());
		params.put("doi", p.getDoi());
		params.put("locked", p.getLocked());
		
		if (sp != null) {
			params.put("limit1", sp.getPage()*sp.getSize());
			params.put("limit2", sp.getSize());
			params.put("orderProperty", sp.getProperty());
			params.put("order", (sp.isAscending()) ? ASC : DESC);
		}
		return publicationMapper.selectByParams(params);
	}


	public int getPublicationsCount() {
		PublicationExample example = new PublicationExample();
		return publicationMapper.countByExample(example);
	}


	public int updatePublicationById(Publication publication) {
		return publicationMapper.updateByPrimaryKey(publication);
	}


	public int deletePublicationById(Integer id) {
		return publicationMapper.deleteByPrimaryKey(id);
	}
	
	public int lockPublications(boolean lockState, List<Publication> pubs) {
		
		List<Integer> ids = new ArrayList<Integer>();
		for (Publication p : pubs) {
			ids.add(p.getId());
		}
		return publicationMapper.lockPublications(lockState, ids);
		
	}

}